/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.ldap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class models an LDAP transaction. Right now it serves the following
 * purposes:
 * <ul>
 * <li>To detect cycles during cascading operations and
 * <li>To handle the rollback of failed transactions.
 * <li>To act as a transaction-scoped cache
 * </ul>
 * The latter is necessary since LDAP doesn't support atomic transactions
 * spanning several entities. In order to perform a rollback, the system has to
 * issue compensating actions in reverse order.
 * 
 * @author levigo
 */
public class Transaction {
	/**
	 * Special attribute ID used to store the a {@link TypeMapping}'s hash code in
	 * a cache element's attributes for later retrieval.
	 */
	private static final String TYPE_MAPPING_KEY = "####TypeMappingKey####";

	private static final Logger logger = LoggerFactory.getLogger(Transaction.class);

	/**
	 * Set of processed entities during a cascading operation. Used to detect
	 * cycles.
	 */
	private final Set processedEntities = new HashSet();

	/**
	 * A list of actions to perform in order to roll back a transaction.
	 */
	private final List<RollbackAction> rollbackActions = new LinkedList<RollbackAction>();

	private final Map<Name, Object> cache = new HashMap<Name, Object>();

	/**
	 * The Mapping that initiated this transaction.
	 */
	private final Mapping mapping;

	/**
	 * The Contexts opened by this transaction.
	 */
	private final Map<DirectoryFacade, DirContext> contextCache = new HashMap<DirectoryFacade, DirContext>();

	private final boolean disableGlobalCache;

	boolean isClosed = false;

	public Transaction(Mapping mapping) {
		// FIXME: (Re)enable caching when fixed
		this(mapping, true);
	}

	public Transaction(Mapping mapping, boolean disableGlobalCache) {
		this.mapping = mapping;
		this.disableGlobalCache = disableGlobalCache;
	}

	/**
	 * Copy constructor. Copies just the ContextFactory from the other
	 * transaction.
	 * 
	 * @param tx
	 */
	public Transaction(Transaction tx) {
		this(tx.mapping, tx.disableGlobalCache);
	}

	/**
	 * Add an entity which has been processed (saved/updated) during the
	 * transaction.
	 * 
	 * @param entity
	 */
	public void addEntity(Object entity) {
		assertNotClosed();

		processedEntities.add(entity);
	}

	/**
	 * Returns whether the given entity has already been processed during the
	 * transaction.
	 * 
	 * @param entity
	 * @return
	 */
	public boolean didAlreadyProcessEntity(Object entity) {
		assertNotClosed();

		return processedEntities.contains(entity);
	}

	/**
	 * Add an action to perform during rollback.
	 * 
	 * @param action
	 */
	public void addRollbackAction(RollbackAction action) {
		rollbackActions.add(action);
	}

	/**
	 * Roll back the transaction by applying all RollbackActions in reverse order.
	 * If one of the actions fail, the rollback continues to undo as much work as
	 * possible.
	 */
	public void rollback() throws RollbackException {
		assertNotClosed();

		try {
			if (logger.isDebugEnabled())
				logger.debug("ROLLBACK: Need to apply " + rollbackActions.size()
						+ " RollbackActions.");

			final ListIterator<RollbackAction> i = rollbackActions
					.listIterator(rollbackActions.size());
			Throwable firstCause = null;
			while (i.hasPrevious()) {
				try {
					i.previous().performRollback();
				} catch (final Throwable e) {
					if (null != firstCause)
						firstCause = e;
					logger
							.error(
									"Exception during Rollback. Trying to continue with rollback anyway.",
									e);
				}

				if (null != firstCause)
					throw new RollbackException(firstCause);
			}
		} finally {
			try {
				closeContexts();
			} catch (final NamingException e) {
				logger.error("Exception during commit - rolling back", e);
			}
		}
	}

	/**
	 * Get an entry from the cache.
	 * 
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public Object getCacheEntry(Name name) throws Exception {
		assertNotClosed();

		final Object cached = cache.get(name);

		if (null != cached) {
			if (logger.isDebugEnabled())
				logger.debug("TX cache hit for " + name);
			return cached;
		}

		if (!disableGlobalCache) {
			// got it in the second level cache?
			final SecondLevelCache slc = mapping.getSecondLevelCache();
			if (null != slc) {
				final Attributes cachedAttributes = slc.getEntry(name);
				if (null != cachedAttributes) {
					if (logger.isDebugEnabled())
						logger.debug("Global cache hit for " + name);

					// re-create a new object instance from the cached attributes.
					final Attribute a = cachedAttributes.get(TYPE_MAPPING_KEY);
					if (null == a)
						// should not happen
						logger.error("No type mapping key in cached attributes");
					else {
						final int hashCode = ((Integer) a.get()).intValue();
						cachedAttributes.remove(TYPE_MAPPING_KEY);

						// find type mapping. FIXME: we may want to get rid of the linear
						// search
						for (final TypeMapping m : mapping.getMappers())
							if (hashCode == m.hashCode()) {
								// resurrect instance from attributes
								final Object instance = m.createInstanceFromAttributes(
										name.toString(), cachedAttributes, this);

								// tx didn't have it yet!
								cache.put(name, instance);

								return instance;
							}
					}
				}
			}
		}

		return null;
	}

	private void assertNotClosed() {
		if (isClosed)
			throw new IllegalStateException("Transaction already closed");
	}

	/**
	 * Put an entry into the cache. This method updates the first-level
	 * (transaction-scoped) cache as well as the second-level (mapping-scoped)
	 * cache.
	 * 
	 * @param m TODO
	 * @param name
	 * @param value
	 */
	public void putCacheEntry(TypeMapping m, Name name, Object value, Attributes a) {
		assertNotClosed();

		cache.put(name, value);

		final SecondLevelCache slc = mapping.getSecondLevelCache();
		if (null != slc) {
			a.put(TYPE_MAPPING_KEY, m.hashCode());
			slc.putEntry(name, a);
		}
	}

	/**
	 * @throws RollbackException
	 * 
	 */
	public void commit() throws RollbackException {
		assertNotClosed();

		try {
			closeContexts();
		} catch (final NamingException e) {
			logger.error("Exception during commit - rolling back", e);
			rollback();
		}
	}

	/**
	 * @throws NamingException
	 * 
	 */
	private void closeContexts() throws NamingException {
		if (contextCache.size() == 0)
			logger.debug("Closed without having opened a Context");

		for (final DirContext ctx : contextCache.values())
			ctx.close();
		contextCache.clear();

		isClosed = true;
	}

	@Override
	protected void finalize() throws Throwable {
		if (contextCache.size() > 0) {
			logger.error("Internal error: disposed incompletely closed Transaction");

			// clean up.
			closeContexts();
		}

		super.finalize();
	}

	/**
	 * @param name
	 */
	public void purgeCacheEntry(Name name) {
		assertNotClosed();

		cache.remove(name);

		final SecondLevelCache slc = mapping.getSecondLevelCache();
		if (null != slc)
			slc.purgeEntry(name);
	}

	public DirContext getContext(DirectoryFacade connectionDescriptor)
			throws DirectoryException {
		assertNotClosed();

		DirContext ctx = contextCache.get(connectionDescriptor);
		if (null == ctx)
			try {
				ctx = openContext(connectionDescriptor);
				contextCache.put(connectionDescriptor, ctx);
				logger.debug("Created a Context for " + connectionDescriptor);
			} catch (final NamingException e) {
				throw new DirectoryException("Can't open connection", e);
			}
		return ctx;
	}

	private DirContext openContext(DirectoryFacade connectionDescriptor)
			throws NamingException {
		final DirContext ctx = connectionDescriptor.createDirContext();

		if (connectionDescriptor.getLDAPEnv().get(
				Mapping.PROPERTY_FORCE_SINGLE_THREADED) != null)
			// Construct a dynamic proxy which forces all calls to the
			// context
			// to happen in a globally synchronized fashion.
			return (DirContext) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class[]{DirContext.class}, new InvocationHandler() {
						public Object invoke(Object proxy, Method method, Object[] args)
								throws Throwable {
							synchronized (Mapping.class) { // sync globally
								try {
									return method.invoke(ctx, args);
								} catch (final Exception e) {
									throw e.getCause();
								}
							}
						};
					});

		return ctx;
	}

	public boolean isClosed() {
		return isClosed;
	}
}

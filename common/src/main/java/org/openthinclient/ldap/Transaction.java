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
import javax.naming.directory.DirContext;

import org.apache.log4j.Logger;

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
	private static final Logger logger = Logger.getLogger(Transaction.class);

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
	private final Map<LDAPConnectionDescriptor, DirContext> contextCache = new HashMap<LDAPConnectionDescriptor, DirContext>();

	private final boolean disableGlobalCache;

	boolean isClosed = false;

	public Transaction(Mapping mapping) {
		this(mapping, false);
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
				logger.debug("Rolling back transaction. Need to apply "
						+ rollbackActions.size() + " RollbackActions.");

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
	 */
	public Object getCacheEntry(Name name) {
		assertNotClosed();

		Object cached = cache.get(name);

		if (null != cached) {
			if (logger.isDebugEnabled())
				logger.debug("TX cache hit for " + name);
			return cached;
		}

		if (!disableGlobalCache) {
			// got it in the mapping cache?
			cached = mapping.getCacheEntry(name);
			if (null != cached) {
				if (logger.isDebugEnabled())
					logger.debug("Global cache hit for " + name);

				// tx didn't have it yet!
				cache.put(name, cached);

				return cached;
			}
		}

		return null;
	}

	private void assertNotClosed() {
		if (isClosed)
			throw new IllegalStateException("Transaction already closed");
	}

	/**
	 * Put an entry into the cache.
	 * 
	 * @param name
	 * @param value
	 */
	public void putCacheEntry(Name name, Object value) {
		assertNotClosed();

		cache.put(name, value);
		mapping.putCacheEntry(name, value);
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
		mapping.purgeCacheEntry(name);
	}

	public DirContext getContext(LDAPConnectionDescriptor connectionDescriptor)
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

	private DirContext openContext(LDAPConnectionDescriptor connectionDescriptor)
			throws NamingException {
		final DirContext ctx = connectionDescriptor.createDirContext();

		if (connectionDescriptor.getExtraEnv().get(
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

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
 *******************************************************************************/
package org.openthinclient.ldap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

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
	 * The Mapping that initialted this transaction.
	 */
	private final Mapping mapping;

	/**
	 * The Contexts opened by this transaction.
	 */
	private Map<Hashtable, DirContext> contextCache = new HashMap<Hashtable, DirContext>();

	private final boolean disableGlobalCache;

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
		this.mapping = tx.mapping;
		this.disableGlobalCache = tx.disableGlobalCache;
	}

	/**
	 * Add an entity which has been processed (saved/updated) during the
	 * transaction.
	 * 
	 * @param entity
	 */
	public void addEntity(Object entity) {
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
		if (logger.isDebugEnabled())
			logger.debug("Rolling back transaction. Need to apply "
					+ rollbackActions.size() + " RollbackActions.");

		ListIterator<RollbackAction> i = rollbackActions
				.listIterator(rollbackActions.size());
		Throwable firstCause = null;
		while (i.hasPrevious()) {
			try {
				i.previous().performRollback();
			} catch (Throwable e) {
				if (null != firstCause)
					firstCause = e;
				logger
						.error(
								"Exception during Rollback. Trying to continue with rollback anyway.",
								e);
			}

			try {
				closeContexts();
			} catch (NamingException e) {
				logger.error("Exception during commit - rolling back", e);
				if (null != firstCause)
					firstCause = e;
			}

			if (null != firstCause)
				throw new RollbackException(firstCause);
		}
	}

	/**
	 * Get an entry from the cache.
	 * 
	 * @param name
	 * @return
	 */
	public Object getCacheEntry(Name name) {
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

	/**
	 * Put an entry into the cache.
	 * 
	 * @param name
	 * @param value
	 */
	public void putCacheEntry(Name name, Object value) {
		cache.put(name, value);
		mapping.putCacheEntry(name, value);
	}

	/**
	 * @return
	 * @throws DirectoryException
	 */
	public DirContext getContext(Class type) throws DirectoryException {
		Hashtable<Object, Object> env = mapping.getEnvPropsByType(type);
		if (null == env)
			env = mapping.getDefaultContextEnvironment();

		return getContext(env);
	}

	private DirContext getContext(Hashtable<Object, Object> env)
			throws DirectoryException {
		DirContext ctx = contextCache.get(env);
		if (null == ctx) {
			ctx = createDirContext(env);
			contextCache.put(env, ctx);
			logger.debug("Created a Context for env " + env);
		}
		return ctx;
	}

	/**
	 * @throws RollbackException
	 * 
	 */
	public void commit() throws RollbackException {
		try {
			closeContexts();
		} catch (NamingException e) {
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

		for (DirContext ctx : contextCache.values()) {
			ctx.close();
		}
		contextCache.clear();
	}

	/**
	 * Return the TypeMapping for a given class.
	 * 
	 * @param c
	 * @return
	 */
	public Mapping getMapping() {
		return mapping;
	}

	/**
	 * @param name
	 */
	public void purgeCacheEntry(Name name) {
		cache.remove(name);
		mapping.purgeCacheEntry(name);
	}

	public DirContext createDirContext(Hashtable<Object, Object> env)
			throws DirectoryException {
		try {
			final DirContext ctx = new InitialDirContext(env);
			if (mapping.getDefaultContextEnvironment().get(
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
									} catch (Exception e) {
										throw e.getCause();
									}
								}
							};
						});
			else
				return ctx;
		} catch (NamingException e) {
			throw new DirectoryException("Can't get context", e);
		}
	}

	public DirContext findContextByDN(String baseDN) throws DirectoryException {
		// Look at all context properties for all mapped classes and determine
		// whether
		// the specified base DN is an absolute DN within the DIT pointed to by the
		// context properties. If we find one, return the corresponding DirContext.
		for (Hashtable<Object, Object> env : mapping.getEnvPropsByType().values()) {
			Object url = env.get("java.naming.provider.url");
			if (null != url) {
				if (baseDN.endsWith(url.toString().substring(
						url.toString().lastIndexOf('/') + 1)))
					return getContext(env);
			}
		}

		// try the default context
		Object url = mapping.getDefaultContextEnvironment().get(
				"java.naming.provider.url");
		if (null != url)
			if (baseDN.endsWith(url.toString().substring(
					url.toString().lastIndexOf('/') + 1)))
				return getContext(mapping.getDefaultContextEnvironment());

		return null;
	}
}

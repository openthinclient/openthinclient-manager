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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.ValidationException;
import org.openthinclient.ldap.TypeMapping.SearchScope;
import org.xml.sax.InputSource;

/**
 * @author levigo
 */
/**
 * @author hennejg
 * 
 */
public class Mapping {
	/**
	 * 
	 */
	public static final String PROPERTY_FORCE_SINGLE_THREADED = "ldap.mapping.single-treaded";

	public static final String MAPPING_DESCRIPTOR_NAME = "org.openthinclient.ldap.mapping.name";

	public static final String MAPPING_IS_MUTABLE = "org.openthinclient.ldap.mapping.mutable";

	private static final Logger logger = Logger.getLogger(Mapping.class);

	private String name;

	private boolean initialized;

	/**
	 * All mappers mamaged by this mapping
	 */
	private final Set<TypeMapping> mappers = new HashSet<TypeMapping>();

	/**
	 * The default type mappers, i.e. the ones to be used, when no explicit target
	 * directory is selected.
	 */
	private final Map<Class, TypeMapping> defaultMappers = new HashMap<Class, TypeMapping>();

	/**
	 * The mappers indexed by mapped type
	 */
	private final Map<Class, Set<TypeMapping>> mappersByType = new HashMap<Class, Set<TypeMapping>>();

	/**
	 * The mappers indexed by connection descriptor (i.e. target Directory Server)
	 */
	private final Map<DirectoryFacade, Set<TypeMapping>> mappersByDirectory = new HashMap<DirectoryFacade, Set<TypeMapping>>();

	private Cache cache;

	public Mapping() {
		try {
			if (CacheManager.getInstance().cacheExists("mapping"))
				cache = CacheManager.getInstance().getCache("mapping");
			else {
				cache = new Cache("mapping", 5000, false, false, 120, 120);
				CacheManager.getInstance().addCache(cache);
			}
		} catch (final CacheException e) {
			logger.error("Can't create cache. Caching is disabled", e);
		}
	}

	public Mapping(Mapping m) {
		this();

		for (final TypeMapping tm : m.mappers)
			try {
				this.add(tm.clone());
			} catch (final CloneNotSupportedException e1) {
				// should not happen. If it does, we're doomed anyway.
				throw new RuntimeException(e1);
			}

		initialize();
	}

	/**
	 * Load an LDAP Mapping
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws MappingException
	 * @throws MarshalException
	 * @throws ValidationException
	 * @throws MarshalException
	 */
	public static Mapping load(InputStream is) throws IOException,
			MappingException, ValidationException, MarshalException {
		// Create a Reader to the file to unmarshal from
		final InputStreamReader reader = new InputStreamReader(is);

		// Create a new Unmarshaller
		final org.exolab.castor.mapping.Mapping m = new org.exolab.castor.mapping.Mapping();
		m.loadMapping(new InputSource(Mapping.class
				.getResourceAsStream("ldap-mapping.xml")));
		final Unmarshaller unmarshaller = new Unmarshaller(m);

		// Unmarshal the configuration object
		final Mapping loadedMapping = (Mapping) unmarshaller.unmarshal(reader);

		return loadedMapping;
	}

	public void add(TypeMapping typeMapping) {
		if (mappers.contains(typeMapping))
			throw new IllegalArgumentException(
					"The specified TypeMapping already contained in this mapping");

		typeMapping.setMapping(this);

		mappers.add(typeMapping);
		defaultMappers.put(typeMapping.getMappedType(), typeMapping);

		// maintain index by class
		Set<TypeMapping> mappersForClass = mappersByType.get(typeMapping
				.getMappedType());
		if (null == mappersForClass) {
			mappersForClass = new HashSet<TypeMapping>();
			mappersByType.put(typeMapping.getMappedType(), mappersForClass);
		}

		mappersForClass.add(typeMapping);

		// maintain index by directory
		final DirectoryFacade lcd = typeMapping.getDirectoryFacade();
		if (null != lcd) {
			Set<TypeMapping> mappersForConnection = mappersByDirectory.get(lcd);
			if (null == mappersForConnection) {
				mappersForConnection = new HashSet<TypeMapping>();
				mappersByDirectory.put(lcd, mappersForConnection);
			}
			mappersForConnection.add(typeMapping);
		}
	}

	public void remove(TypeMapping tm) {
		if (!mappers.remove(tm))
			return;

		defaultMappers.remove(tm.getMappedType());

		// maintain index by type
		final Set<TypeMapping> forType = mappersByType.get(tm.getMappedType());
		if (null != forType) {
			forType.remove(tm);
			if (forType.isEmpty())
				mappersByType.remove(tm.getMappedType());
		}

		// maintain index by connection
		final Set<TypeMapping> forConnection = mappersByDirectory.get(tm
				.getDirectoryFacade());
		if (null != forConnection) {
			forConnection.remove(tm);
			if (forConnection.isEmpty())
				mappersByType.remove(tm.getDirectoryFacade());
		}
	}

	public void initialize() {
		if (initialized)
			return;

		for (final TypeMapping m : defaultMappers.values())
			m.initPostLoad();

		initialized = true;

		if (logger.isTraceEnabled())
			logger.trace("LDAP mapping initialized");
	}

	/**
	 * Set the JNDI environment properties for all contained {@link TypeMapping}.
	 * 
	 * @param env
	 */
	public void setDirectoryFacade(DirectoryFacade lcd) {
		// iterate over a copy to prevent a CME
		for (final TypeMapping tm : new ArrayList<TypeMapping>(mappers)) {
			// remove and add to preserve mapping indexes
			remove(tm);
			tm.setDirectoryFacade(lcd);
			add(tm);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public <T> Set<T> list(Class<T> type) throws DirectoryException {
		if (!initialized)
			throw new DirectoryException(
					"Mapping is not yet initialized - call initialize() first");

		return list(type, null, null, null);
	}

	/**
	 * List objects of the given type at or below the given search base using the
	 * given context.
	 * 
	 * @param <T>
	 * @param type
	 * @param filter
	 * @param baseDN
	 * @param scope
	 * @return
	 * @throws DirectoryException
	 */
	@SuppressWarnings("cast")
	public <T> Set<T> list(Class<T> type, Filter filter, String baseDN,
			SearchScope scope) throws DirectoryException {
		if (!initialized)
			throw new DirectoryException(
					"Mapping is not yet initialized - call initialize() first");

		if (logger.isDebugEnabled())
			logger.debug("list(): type=" + type + ", filter=" + filter
					+ ", searchBase=" + baseDN);

		// get mapper. try to find one for the specified search base first
		TypeMapping tm;
		try {
			tm = getMapping(type, baseDN);
		} catch (final NamingException e) {
			throw new DirectoryException(
					"Can't determine TypeMapping for this type and search base", e);
		}

		// fall back to default mapping if not found
		if (null == tm)
			tm = defaultMappers.get(type);

		if (null == tm)
			throw new IllegalArgumentException("No mapping for class " + type);

		final Transaction tx = new Transaction(this);
		try {
			return (Set<T>) tm.list(filter, baseDN, scope, tx);
		} catch (final DirectoryException e) {
			tx.rollback();
			throw e;
		} catch (final RuntimeException e) {
			tx.rollback();
			throw e;
		} finally {
			if (!tx.isClosed())
				tx.commit();
		}
	}

	/**
	 * Find the TypeMapping to use for a given mapped type. Refined by base DN if
	 * appropriate/necessary.
	 * 
	 * @param type the mapped type
	 * @param baseDN the base DN of the object to be handled or <code>null</code>
	 *          if it is not (yet?) known.
	 * 
	 * @return
	 * @throws NamingException
	 */
	TypeMapping getMapping(Class type, String baseDN) throws NamingException {
		// no base DN -> use default mapping
		if (null == baseDN)
			return defaultMappers.get(type);

		// try to find suitable mapping, assuming that the base DN is absolute
		final Set<TypeMapping> mappersForClass = mappersByType.get(type);
		for (final TypeMapping tm : mappersForClass)
			if (tm.getDirectoryFacade().contains(
					tm.getDirectoryFacade().getNameParser().parse(baseDN)))
				return tm;

		// no cigar? fall back to default
		return defaultMappers.get(type);
	}

	/**
	 * @param object
	 * @return
	 * @throws DirectoryException
	 */
	public <T> T load(Class<T> type, String dn, boolean noCache)
			throws DirectoryException {

		// memberClass = type.toString();

		if (!initialized)
			throw new DirectoryException(
					"Mapping is not yet initialized - call initialize() first");

		if (logger.isDebugEnabled())
			logger.debug("load(): type=" + type + ", dn=" + dn);

		TypeMapping tm;
		try {
			tm = getMapping(type, dn);
		} catch (final NamingException e) {
			throw new DirectoryException(
					"Can't determine TypeMapping for this type and DN", e);
		}

		if (null == tm)
			throw new IllegalArgumentException("No mapping for class " + type);

		final Transaction tx = new Transaction(this, noCache);
		try {
			return (T) tm.load(dn, tx);
		} catch (final DirectoryException e) {
			tx.rollback();
			throw e;
		} catch (final RuntimeException e) {
			tx.rollback();
			throw e;
		} finally {
			if (!tx.isClosed())
				tx.commit();
		}
	}

	/*
	 * @see org.openthinclient.common.directory.Directory#create(java.lang.Class,
	 *      java.lang.String)
	 */
	public <T> T create(Class<T> type) throws DirectoryException {
		if (logger.isDebugEnabled())
			logger.debug("create(): create=" + type);

		final TypeMapping tm = defaultMappers.get(type);
		if (null == tm)
			throw new IllegalArgumentException("No mapping for class " + type);

		return (T) tm.create();
	}

	/*
	 * @see org.openthinclient.common.directory.Directory#delete(org.openthinclient.common.directory.DirectoryObject)
	 */
	public boolean delete(Object object) throws DirectoryException {
		if (logger.isDebugEnabled())
			logger.debug("delete(): type=" + object.getClass());

		final TypeMapping tm = defaultMappers.get(object.getClass());
		if (null == tm)
			throw new IllegalArgumentException("No mapping for class "
					+ object.getClass());

		final Transaction tx = new Transaction(this);
		try {
			return tm.delete(object, tx);
		} catch (final DirectoryException e) {
			tx.rollback();
			throw e;
		} catch (final RuntimeException e) {
			tx.rollback();
			throw e;
		} finally {
			if (!tx.isClosed())
				tx.commit();
		}
	}

	/*
	 * @see org.openthinclient.common.directory.Directory#save(org.openthinclient.common.directory.DirectoryObject)
	 */
	public void save(Object o, String baseDN) throws DirectoryException {

		if (!initialized)
			throw new DirectoryException(
					"Mapping is not yet initialized - call initialize() first");

		if (logger.isDebugEnabled())
			logger.debug("save(): object=" + o + ", baseDN=" + baseDN);
		final TypeMapping tm = defaultMappers.get(o.getClass());
		if (null == tm)
			throw new IllegalArgumentException("No mapping for class " + o.getClass());

		final Transaction tx = new Transaction(this);
		try {
			tm.save(o, baseDN, tx);
		} catch (final DirectoryException e) {
			tx.rollback();
			throw e;
		} catch (final RuntimeException e) {
			tx.rollback();
			throw e;
		} finally {
			if (!tx.isClosed())
				tx.commit();
		}
	}

	/**
	 * @return
	 */
	public Map<Class, TypeMapping> getTypes() {
		return Collections.unmodifiableMap(defaultMappers);
	}

	/**
	 * Return the (default) TypeMapping for a given class.
	 * 
	 * @param c
	 * @return
	 */
	TypeMapping getMapping(Class c) {
		return defaultMappers.get(c);
	}

	/**
	 * Return the mapping for the object at a given DN. In order to determine the
	 * mapping, the object's objectClasses need to be loaded.
	 * 
	 * @param dn
	 * @param objectClasses
	 * @param tx current transaction
	 * @return
	 * @throws DirectoryException
	 * @throws NamingException
	 */
	TypeMapping getMapping(String dn, Transaction tx) throws DirectoryException,
			NamingException {
		for (final Map.Entry<DirectoryFacade, Set<TypeMapping>> e : mappersByDirectory
				.entrySet()) {
			// check whether the directory contains the dn
			final DirectoryFacade df = e.getKey();
			final Name parsedDN = df.getNameParser().parse(dn);
			if (df.contains(parsedDN)) {
				// load the object and determine the object class
				final DirContext ctx = tx.getContext(df);
				final String[] attributes = {"objectClass"};
				final Attributes a = ctx.getAttributes(df.makeRelativeName(dn),
						attributes);
				final Attribute objectClasses = a.get("objectClass");

				// build list of mapping candidates. There may be more than one!
				final List<TypeMapping> candidates = new ArrayList<TypeMapping>();
				for (final TypeMapping tm : e.getValue())
					if (tm.matchesKeyClasses(objectClasses))
						candidates.add(tm);

				// if there is only one match, return it
				if (candidates.size() == 1)
					return candidates.get(0);

				// if more than one match, select best one by base RDN
				for (final TypeMapping tm : candidates)
					if (tm.getBaseRDN() != null)
						if (parsedDN.startsWith(tm.getDefaultBaseName()))
							return tm;

				// no "best" match -> just use first one
				if (candidates.size() > 0)
					return candidates.get(0);
			}
		}

		return null;
	}

	/**
	 * @param a
	 * @throws DirectoryException
	 */
	public void refresh(Object o) throws DirectoryException {
		if (!initialized)
			throw new DirectoryException(
					"Mapping is not yet initialized - call initialize() first");

		if (logger.isDebugEnabled())
			logger.debug("refresh(): object=" + o);

		final TypeMapping tm = defaultMappers.get(o.getClass());
		if (null == tm)
			throw new IllegalArgumentException("No mapping for class " + o.getClass());

		final Transaction tx = new Transaction(this);
		try {
			tm.refresh(o, tx);
		} catch (final DirectoryException e) {
			tx.rollback();
			throw e;
		} catch (final RuntimeException e) {
			tx.rollback();
			throw e;
		} finally {
			if (!tx.isClosed())
				tx.commit();
		}
	}

	/**
	 * Close this mapping by closing all cached contexts. The mapping may still be
	 * used afterwards.
	 */
	public void close() {
		// FIXME
	}

	/**
	 * @param name
	 * @param object
	 */
	void putCacheEntry(Name name, Object object) {
		if (null != cache) {
			cache.put(new Element(name, (Serializable) object));
			if (logger.isTraceEnabled())
				logger.trace("Caching entry for " + name);
		}
	}

	/**
	 * @param name
	 * @return
	 */
	Object getCacheEntry(Name name) {
		if (null == cache)
			return null;
		try {
			final Element element = cache.get(name);
			if (null != element && logger.isTraceEnabled())
				logger.trace("Global cache hit for " + name);
			return null != element ? element.getValue() : null;
		} catch (final Throwable e) {
			logger.warn("Can't get from cache", e);
			return null;
		}
	}

	/**
	 * @param name
	 * @return
	 * @throws IllegalStateException
	 */
	boolean purgeCacheEntry(Name name) throws IllegalStateException {
		if (null != cache)
			return cache.remove(name);
		return false;
	}

	void clearCache() throws IllegalStateException, IOException {
		if (null != cache)
			cache.removeAll();
	}

	/**
	 * Find the TypeMapping to use for a given mapped type where the connection
	 * descriptor is the same as the specified one.
	 * 
	 * @param type the mapped type
	 * @param baseDN the base DN of the object to be handled or <code>null</code>
	 *          if it is not (yet?) known.
	 * 
	 * @return
	 * @throws NamingException
	 */
	TypeMapping getMapping(Class type, DirectoryFacade connectionDescriptor) {
		final Set<TypeMapping> mappers = mappersByDirectory
				.get(connectionDescriptor);
		for (final TypeMapping tm : mappers)
			if (tm.getMappedType().equals(type))
				return tm;

		throw new IllegalArgumentException(
				"No mapping for the specified type and connection descriptor");
	}

	Set<TypeMapping> getMappers() {
		return mappers;
	}

	/**
	 * Clear/update all references to the specified dn. This is usually used in
	 * response to an object deletion/rename.
	 * 
	 * @param tx
	 * @param oldDN the name of the existing object being referred to
	 * @param newDN the new name of the object, or <code>null</code> if the
	 *          object has been deleted.
	 * @throws DirectoryException
	 * @throws NamingException
	 */
	void updateReferences(Transaction tx, String oldDN, String newDN)
			throws DirectoryException, NamingException {
		// iterate over target directories, so that we can query the referrers
		// efficiently using just one query per directory.
		for (final Map.Entry<DirectoryFacade, Set<TypeMapping>> e : mappersByDirectory
				.entrySet()) {
			final Set<TypeMapping> mappers = e.getValue();
			final DirectoryFacade directory = e.getKey();

			// Build list of referrer attributes.
			final Set<String> refererAttributes = new HashSet<String>();
			for (final TypeMapping m : mappers)
				m.collectRefererAttributes(refererAttributes);

			final DirContext ctx = tx.getContext(directory);
			final StringBuilder sb = new StringBuilder("(|");
			for (final String a1 : refererAttributes)
				sb.append("(").append(a1).append("=").append(oldDN).append(")");
			sb.append(")");

			// we query by referrer attribute name only. Theoretically, we would also
			// need to use the object class in the query, but we can probably get
			// away with this simplification in all practical cases.
			final SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			sc.setReturningAttributes(new String[refererAttributes.size()]);
			sc.setDerefLinkFlag(false);

			// issue query
			final NamingEnumeration<SearchResult> ne = ctx.search("", sb.toString(),
					sc);

			while (ne.hasMore()) {
				final SearchResult result = ne.next();
				final Attributes attributes = result.getAttributes();
				List<ModificationItem> mods = null;
				for (final String a : refererAttributes)
					if (attributes.get(a) != null) {
						if (null == mods)
							mods = new LinkedList<ModificationItem>();
						mods.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE,
								new BasicAttribute(a, oldDN)));

						// for rename: re-add new name
						if (null != newDN)
							mods.add(new ModificationItem(DirContext.ADD_ATTRIBUTE,
									new BasicAttribute(a, newDN)));
					}

				if (null != mods) {
					if (logger.isDebugEnabled()) {
						if (logger.isDebugEnabled())
							logger.debug("   CASCADING UPDATE " + result.getName());
						for (final ModificationItem mi : mods)
							logger.debug("      - " + mi);
					}

					ctx.modifyAttributes(result.getName(), mods
							.toArray(new ModificationItem[mods.size()]));
				}
			}
		}
	}
}

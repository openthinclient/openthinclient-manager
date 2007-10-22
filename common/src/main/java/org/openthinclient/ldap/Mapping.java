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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

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
	 * The default type mappers, i.e. the ones to be used, when no explicit target
	 * directory is selected.
	 */
	private Map<Class, TypeMapping> defaultMappers = new HashMap<Class, TypeMapping>();

	/**
	 * A map of lists of type mappers indexed by directory base name.
	 */
	private Map<Name, Map<Class, TypeMapping>> mappersByDirectory = new HashMap<Name, Map<Class, TypeMapping>>();

	private Hashtable<Object, Object> defaultContextEnvironment;

	private Map<Class, Hashtable<Object, Object>> envPropsByType = new HashMap<Class, Hashtable<Object, Object>>();

	private Cache cache;

	public Mapping() {
		try {
			if (CacheManager.getInstance().cacheExists("mapping"))
				cache = CacheManager.getInstance().getCache("mapping");
			else {
				cache = new Cache("mapping", 5000, false, false, 120, 120);
				CacheManager.getInstance().addCache(cache);
			}
		} catch (CacheException e) {
			logger.error("Can't create cache. Caching is disabled", e);
		}
	}

	public Mapping(Mapping m) {
		this();

		for (TypeMapping tm : m.defaultMappers.values()) {
			try {
				this.add(tm.clone());
			} catch (CloneNotSupportedException e1) {
				// should not happen. If it does, we're doomed anyway.
				throw new RuntimeException(e1);
			}
		}

		this.defaultContextEnvironment = m.defaultContextEnvironment;

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
		InputStreamReader reader = new InputStreamReader(is);

		// Create a new Unmarshaller
		org.exolab.castor.mapping.Mapping m = new org.exolab.castor.mapping.Mapping();
		m.loadMapping(new InputSource(Mapping.class
				.getResourceAsStream("ldap-mapping.xml")));
		Unmarshaller unmarshaller = new Unmarshaller(m);

		// Unmarshal the configuration object
		Mapping loadedMapping = (Mapping) unmarshaller.unmarshal(reader);

		return loadedMapping;
	}

	public void add(TypeMapping typeMapping) {
		defaultMappers.put(typeMapping.getModelClass(), typeMapping);
		typeMapping.setMapping(this);
	}

	public void remove(Class type) {
		defaultMappers.remove(type);
	}

	public void initialize() {
		if (initialized)
			return;

		for (TypeMapping m : defaultMappers.values())
			m.initPostLoad();

		initialized = true;

		if (logger.isDebugEnabled())
			logger.debug("LDAP mapping initialized");
	}

	public void setDefaultContextEnvironment(Hashtable<Object, Object> ctx) {
		// Enable connection pooling
		ctx.put("com.sun.jndi.ldap.connect.pool", "true");
		ctx.put(Context.BATCHSIZE, "100");

		this.defaultContextEnvironment = ctx;
	}

	public Hashtable<Object, Object> getDefaultContextEnvironment() {
		return defaultContextEnvironment;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {

		if (logger.isDebugEnabled())
			logger.debug("The server of this connection: " + name);

		this.name = name;
	}

	public <T> Set<T> list(Class<T> type) throws DirectoryException {
		if (!initialized)
			throw new DirectoryException(
					"Mapping is not yet initialized - call initialize() first");

		return list(type, null, null, null, null);
	}

	public <T> Set<T> list(Class<T> type, String baseDN)
			throws DirectoryException {
		if (!initialized)
			throw new DirectoryException(
					"Mapping is not yet initialized - call initialize() first");

		return list(type, null, null, baseDN, null);
	}

	@SuppressWarnings("cast")
	public <T> Set<T> list(Class<T> type, DirContext ctx, Filter filter,
			String searchBase, SearchScope scope) throws DirectoryException {
		if (!initialized)
			throw new DirectoryException(
					"Mapping is not yet initialized - call initialize() first");

		if (logger.isDebugEnabled())
			logger.debug("list(): type=" + type + ", ctx=" + ctx + ", filter="
					+ filter + ", searchBase=" + searchBase);

		// get mapper
		
		TypeMapping tm = defaultMappers.get(type);
		if (null == tm)
			throw new IllegalArgumentException("No mapping for class " + type);

		Transaction tx = new Transaction(this);
		try {
			return (Set<T>) tm.list(filter, searchBase, scope, tx);
		} catch (DirectoryException e) {
			tx.rollback();
			throw e;
		} catch (RuntimeException e) {
			tx.rollback();
			throw e;
		} finally {
			tx.commit();
		}
	}

	/**
	 * @param ctx
	 * @param object
	 * @return
	 * @throws DirectoryException
	 */
	public <T> T load(Class<T> type, DirContext ctx, String dn, boolean noCache)
			throws DirectoryException {

		// memberClass = type.toString();

		if (!initialized)
			throw new DirectoryException(
					"Mapping is not yet initialized - call initialize() first");

		if (logger.isDebugEnabled())
			logger.debug("load(): type=" + type + ", ctx=" + ctx + ", dn=" + dn);

		TypeMapping tm = defaultMappers.get(type);
		if (null == tm)
			throw new IllegalArgumentException("No mapping for class " + type);

		final Transaction tx = new Transaction(this, noCache);
		try {
			return (T) tm.load(dn, tx);
		} catch (DirectoryException e) {
			tx.rollback();
			throw e;
		} catch (RuntimeException e) {
			tx.rollback();
			throw e;
		} finally {
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

		TypeMapping tm = defaultMappers.get(type);
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

		TypeMapping tm = defaultMappers.get(object.getClass());
		if (null == tm)
			throw new IllegalArgumentException("No mapping for class "
					+ object.getClass());

		final Transaction tx = new Transaction(this);
		try {
			return tm.delete(object, tx);
		} catch (DirectoryException e) {
			tx.rollback();
			throw e;
		} catch (RuntimeException e) {
			tx.rollback();
			throw e;
		} finally {
			tx.commit();
		}
	}

	/*
	 * @see org.openthinclient.common.directory.Directory#save(org.openthinclient.common.directory.DirectoryObject)
	 */
	public void save(Object o, DirContext ctx, String baseDN)
			throws DirectoryException {

		if (!initialized)
			throw new DirectoryException(
					"Mapping is not yet initialized - call initialize() first");

		if (logger.isDebugEnabled())
			logger.debug("save(): object=" + o + ", ctx=" + ctx + ", baseDN="
					+ baseDN);
		TypeMapping tm = defaultMappers.get(o.getClass());
		if (null == tm)
			throw new IllegalArgumentException("No mapping for class " + o.getClass());

		final Transaction tx = new Transaction(this);
		try {
			tm.save(o, baseDN, tx);
		} catch (DirectoryException e) {
			tx.rollback();
			throw e;
		} catch (RuntimeException e) {
			tx.rollback();
			throw e;
		} finally {
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
	 * Return the TypeMapping for a given class.
	 * 
	 * @param c
	 * @return
	 */
	public TypeMapping getMapping(Class c) {
		return defaultMappers.get(c);
	}

	/**
	 * Set/add a type mapping. The added mapping will replace an existing mapping
	 * for the type it maps to.
	 * 
	 * @param m
	 */
	public void putMapping(TypeMapping m) {
		m.setMapping(this);
		defaultMappers.put(m.getModelClass(), m);
	}
	
	/**
	 * @param memberDN
	 * @param objectClasses
	 * @param tx TODO
	 * @return
	 * @throws DirectoryException
	 * @throws NamingException
	 */
	TypeMapping getMappingByDN(String memberDN, Attribute objectClasses,
			Transaction tx) throws DirectoryException, NamingException {
		for (TypeMapping tm : defaultMappers.values()) {
			// we don't, for now, support cases where a mapping type pointed to
			// by a group mapping doesn't specify a base dn.
			if (null == tm.getBaseDN()) {
						continue;
			}

			// FIXME: cache tmName in TM.
			DirContext ctx = null;
			
			memberDN = TypeMapping.idToLowerCase(memberDN);
			
			if (null != memberDN)
				ctx = tx.findContextByDN(memberDN);

			if(null == ctx)
				ctx = tx.getContext(tm.getModelClass());

			NameParser np = ctx.getNameParser("");
			Name tmName = np.parse(ctx.getNameInNamespace()).addAll(
					np.parse(tm.getBaseDN()));
			Name memberName = np.parse(memberDN);
			Name memberNameSuffix = memberName.getPrefix(memberName.size() - 1);

			
			if (memberNameSuffix.equals(tmName))
				return tm;
		}

		return null;
	}

	/**
	 * @param memberDN
	 * @param objectClasses
	 * @param tx TODO
	 * @return
	 * @throws DirectoryException
	 * @throws NamingException
	 */
	TypeMapping getMappingByAttributes(String memberDN, Attribute objectClasses,
			Transaction tx) throws DirectoryException, NamingException {
		memberDN = TypeMapping.idToLowerCase(memberDN);

		DirContext ctx = tx.findContextByDN(memberDN);
		if (null == ctx) {
			logger.error("Can't find a directory context for " + memberDN);
			return null;
		}

		NameParser np = ctx.getNameParser("");
		Name memberName = np.parse(memberDN);

		String searchDN = memberDN.replace("," + ctx.getNameInNamespace(), ""); //FIXME: remove or change

		Name searchName = np.parse(searchDN);

		// load objectClass attribute of referenced object
		Attributes attributes = ctx.getAttributes(searchName,
				new String[]{"objectClass"});

		if (null == attributes) {
			logger.error(memberDN + " missing objectClass attribute");
			return null;
		}

		// retrieve object classes for the object pointed to by the group entry
		Attribute a = attributes.get("objectClass");

		String memberObjectClasses[] = new String[a.size()];
		for (int i = 0; i < memberObjectClasses.length; i++)
			memberObjectClasses[i] = (String) a.get(i);
		Arrays.sort(memberObjectClasses);

		for (Map.Entry<Name, Map<Class, TypeMapping>> mappersForDirectory : mappersByDirectory
				.entrySet()) {

			if (memberName.toString().endsWith(
					mappersForDirectory.getKey().toString())) {

				for (TypeMapping tm : mappersForDirectory.getValue().values()) {
					// determine whether the found member has the same object classes as
					// the
					// mapping
//					String[] mappingClasses = tm.getObjectClasses();
//					Arrays.sort(mappingClasses);
					String keyClass = tm.getKeyClass();
					boolean keyClassFound=false;

					for(String objectClass : memberObjectClasses) {
						if(objectClass.equals(keyClass))
								keyClassFound = true;
					}
					
					if(false == keyClassFound)
						continue;		
//				if (!Arrays.equals(mappingClasses, memberObjectClasses))
//						continue;

					return tm;
				}
			}
		}

		return null;

		// for (TypeMapping tm : defaultMappers.values()) {
		// // determine whether the found member has the same object classes as the
		// // mapping
		// String[] mappingClasses = tm.getObjectClasses();
		// Arrays.sort(mappingClasses);
		// if (!Arrays.equals(mappingClasses, memberObjectClasses))
		// continue;
		//
		// // FIXME: determine whether the mapping points to the same context
		// // (i.e. has the same connection properties) as the one we found to be
		// // responsible for the member DN.
		//
		// return tm;
		// // // we don't, for now, support cases where a mapping type pointed to
		// // // by a group mapping doesn't specify a base dn.
		// // if (null == tm.getBaseDN()) {
		// // continue;
		// // }
		// //
		// // // FIXME: cache tmName in TM.
		// // DirContext ctx = ctx;
		// //
		// // if (null == ctx)
		// // ctx = tx.getContext(tm.getModelClass());
		// //
		// // // determine if one name lies within the other, i.e.
		// // // rdn=a,rdn=b,rdn=c lies within rdn=b,rdn=c
		// // NameParser np = ctx.getNameParser("");
		// // Name tmName = np.parse(ctx.getNameInNamespace()).addAll(
		// // np.parse(tm.getBaseDN()));
		// // Name memberName = np.parse(memberDN);
		// //
		// // int len = Math.min(memberName.size(), tmName.size());
		// //
		// // if (!memberName.getPrefix(len).equals(tmName.getPrefix(len)))
		// // continue;
		//
		// }
		//
		// return null;
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

		TypeMapping tm = defaultMappers.get(o.getClass());
		if (null == tm)
			throw new IllegalArgumentException("No mapping for class " + o.getClass());

		final Transaction tx = new Transaction(this);
		try {
			tm.refresh(o, tx);
		} catch (DirectoryException e) {
			tx.rollback();
			throw e;
		} catch (RuntimeException e) {
			tx.rollback();
			throw e;
		} finally {
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
			if (logger.isDebugEnabled())
				logger.debug("Caching entry for " + name);
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
			Element element = cache.get(name);
			if (null != element && logger.isDebugEnabled())
				logger.debug("Global cache hit for " + name);
			return null != element ? element.getValue() : null;
		} catch (Throwable e) {
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

	public void setEnvPropsForType(Class type, Hashtable<Object, Object> envProps) {
		envPropsByType.put(type, envProps);
	}

	public Hashtable<Object, Object> getEnvPropsByType(Class type) {
		return envPropsByType.get(type);
	}

	Map<Class, Hashtable<Object, Object>> getEnvPropsByType() {
		return envPropsByType;
	}

	public Map<Class, TypeMapping> getMappersByDirectory(Name name) {
		return mappersByDirectory.get(name);
	}

	public void setMappersByDirectory(Name name, Map<Class, TypeMapping> mappers) {
		mappersByDirectory.put(name, mappers);
	}
}

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

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
	 * For unit-test purposes only...
	 */
	public static boolean disableCache = false;

	private static final Logger logger = Logger.getLogger(Mapping.class);

	/**
	 * Property key to be used when all directory operations should be forced into
	 * a single-threaded access model. If this property is set to a non-<code>null</code>
	 * value, all accesses are synchronized. This will limit the number of
	 * parallel directory operations effected by the mapping to one.
	 */
	public static final String PROPERTY_FORCE_SINGLE_THREADED = "ldap.mapping.single-treaded";

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

	/**
	 * The default type mappers, i.e. the ones to be used, when no explicit target
	 * directory is selected.
	 */
	private final Map<Class, TypeMapping> defaultMappers = new HashMap<Class, TypeMapping>();

	private boolean initialized;

	/**
	 * All mappers mamaged by this mapping
	 */
	private final Set<TypeMapping> mappers = new HashSet<TypeMapping>();

	/**
	 * The mappers indexed by connection descriptor (i.e. target Directory Server)
	 */
	private final Map<DirectoryFacade, Set<TypeMapping>> mappersByDirectory = new HashMap<DirectoryFacade, Set<TypeMapping>>();

	/**
	 * The mappers indexed by mapped type
	 */
	private final Map<Class, Set<TypeMapping>> mappersByType = new HashMap<Class, Set<TypeMapping>>();

	/**
	 * The Mapping's name.
	 */
	private String name;

	// FIXME: make this configurable
	private final SecondLevelCache secondLevelCache = new EhCacheSecondLevelCache();

	public Mapping() {

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
	 * Add a type mapping.
	 * 
	 * @param typeMapping
	 */
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

	/**
	 * Close this mapping. Closing a mapping currently has the sole effect of
	 * purging the cache.
	 */
	public void close() {
		try {
			if (null != secondLevelCache)
				secondLevelCache.clear();
		} catch (final Exception e) {
			logger.error("Can't purge cache", e);
		}
	}

	/**
	 * Create an object (new instance) of the given type and initialize the RDN
	 * atttribute.
	 * 
	 * @param type
	 * @return
	 * @throws DirectoryException
	 */
	public <T> T create(Class<T> type) throws DirectoryException {
		if (logger.isDebugEnabled())
			logger.debug("create(): create=" + type);

		final TypeMapping tm = defaultMappers.get(type);
		if (null == tm)
			throw new IllegalArgumentException("No mapping for class " + type);

		return (T) tm.create();
	}

	/**
	 * Remove the given object from the directory.
	 * 
	 * @param object
	 * @throws DirectoryException
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

	/**
	 * Get set of {@link TypeMapping}s managed by this Mapping.
	 * 
	 * @return
	 */
	Set<TypeMapping> getMappers() {
		return mappers;
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

				DiropLogger.LOG.logGetAttributes(dn, attributes, "determining mapping");

				final Attributes a = ctx.getAttributes(df.makeRelativeName(dn),
						attributes);
				final Attribute objectClasses = a.get("objectClass");

				final Set<TypeMapping> mappings = e.getValue();

				final TypeMapping match = getMapping(parsedDN, objectClasses, mappings);
				if (null != match)
					return match;
			}
		}

		return null;
	}

	/**
	 * Find the best TypeMapping for an object described by its DN an object
	 * classes from a set of mappings.
	 * 
	 * @param parsedDN
	 * @param objectClasses
	 * @param mappings
	 * @return
	 * @throws NamingException
	 * @throws InvalidNameException
	 */
	private TypeMapping getMapping(final Name parsedDN,
			final Attribute objectClasses, Set<TypeMapping> mappings)
			throws NamingException, InvalidNameException {
		// build list of mapping candidates. There may be more than one!
		final List<TypeMapping> candidates = new ArrayList<TypeMapping>();
		for (final TypeMapping tm : mappings)
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

		return null;
	}

	/**
	 * Get a map of {@link TypeMapping}s by mapped class.
	 * 
	 * @return
	 */
	public Map<Class, TypeMapping> getTypes() {
		return Collections.unmodifiableMap(defaultMappers);
	}

	/**
	 * Initialize this Mapping. Used after unmarshalling it from XML.
	 */
	public void initialize() {
		if (initialized)
			return;

		for (final TypeMapping m : defaultMappers.values())
			m.initPostLoad();

		initialized = true;

		if (logger.isDebugEnabled())
			logger.debug("LDAP mapping initialized");
	}

	/**
	 * List all objects of the given type located at the default base DN for the
	 * given type.
	 * 
	 * @param type
	 * @return
	 * @throws DirectoryException
	 */
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
	 * Load an object of the given type from the given dn.
	 * 
	 * @param type the (expected) type
	 * @param dn the object's dn
	 * @return
	 * @throws DirectoryException
	 */
	public <T> T load(Class<T> type, String dn) throws DirectoryException {
		return load(type, dn, false);
	}

	/**
	 * Load an object of the given type from the given dn.
	 * 
	 * @param type the (expected) type
	 * @param dn the object's dn
	 * @param noCache if <code>true</code> the cache will not be consulted for
	 *          this object.
	 * @return
	 * @throws DirectoryException
	 */
	public <T> T load(Class<T> type, String dn, boolean noCache)
			throws DirectoryException {
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

	/**
	 * Refresh the given object's state from the directory. The object must
	 * already have a DN for this operation to succeed. This operations always
	 * by-passes the cache, making sure that the object's state after the refresh
	 * is consistent with the directory.
	 * 
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

		final Transaction tx = new Transaction(this, true);
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
	 * Remove the given {@link TypeMapping}.
	 * 
	 * @param tm
	 */
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
				mappersByDirectory.remove(tm.getDirectoryFacade());
		}
	}

	/**
	 * Save the given object to the default base DN appropriate for the given
	 * object type.
	 * 
	 * @param o the object to be saved
	 * @throws DirectoryException
	 */
	public void save(Object o) throws DirectoryException {
		save(o, null);
	}

	/**
	 * Save the given object to the given base DN. The object DN will be made up
	 * from the base DN and the object's RDN. If the object was already persistent
	 * and is therefore only updated, specifying the base DN will have no effect.
	 * 
	 * @param o the object to be saved
	 * @param baseDN the base DN at which to save the object
	 * @throws DirectoryException
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
	 * Set the directory connection to be used.
	 * 
	 * @param lcd
	 * 
	 * @see #setDirectoryFacade(DirectoryFacade)
	 */
	public void setConnectionDescriptor(LDAPConnectionDescriptor lcd) {
		setDirectoryFacade(lcd.createDirectoryFacade());
	}

	/**
	 * Set the {@link DirectoryFacade} to be used for all accesses to the
	 * directory. This method may be used instead of
	 * {@link #setConnectionDescriptor(LDAPConnectionDescriptor)} if a
	 * {@link DirectoryFacade} has already been obtained otherwise.
	 * 
	 * @param env
	 * 
	 * @see #setConnectionDescriptor(LDAPConnectionDescriptor)
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
			final Set<ReferenceAttributeMapping> refererAttributes = new HashSet<ReferenceAttributeMapping>();
			for (final TypeMapping m : mappers)
				m.collectRefererAttributes(refererAttributes);

			// compress references into set of attribute names and a set of type
			// mappings (not all types have references at all!)
			final Set<String> attributeNames = new HashSet<String>();
			final Set<TypeMapping> effectiveMappers = new HashSet<TypeMapping>();
			for (final ReferenceAttributeMapping ra : refererAttributes) {
				attributeNames.add(ra.getFieldName());
				effectiveMappers.add(ra.getTypeMapping());
			}

			// build filter expression
			final DirContext ctx = tx.getContext(directory);
			final StringBuilder sb = new StringBuilder("(|");
			for (final String name : attributeNames)
				sb.append("(").append(name).append("=").append(oldDN).append(")");
			sb.append(")");

			// we query by referrer attribute name only. Theoretically, we would also
			// need to use the object class in the query, but we can probably get
			// away with this simplification in all practical cases.
			final SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
			sc.setReturningAttributes(new String[refererAttributes.size()]);
			sc.setDerefLinkFlag(false);

			final String filter = sb.toString();

			DiropLogger.LOG.logSearch("", filter, null, sc, "searching references");

			// issue query to find referencing objects
			final NamingEnumeration<SearchResult> ne = ctx.search("", filter, sc);

			while (ne.hasMore()) {
				final SearchResult result = ne.next();
				final Attributes attributes = result.getAttributes();
				List<ModificationItem> mods = null;

				// Determine applicable TypeMapper for the referencing object
				final TypeMapping m = getMapping(directory.makeAbsoluteName(result
						.getName()), attributes.get("objectClass"), mappers);

				if (null == m) {
					logger
							.warn("Could not determine TypeMapping for referencing object at "
									+ result.getName());
					continue;
				}

				for (final ReferenceAttributeMapping ra : refererAttributes) {
					// check whether the reference matches the type of object we found
					if (ra.getTypeMapping() != m)
						continue;

					final Attribute attr = attributes.get(ra.getFieldName());
					if (attr != null) {
						// for rename: re-add new name
						if (null != newDN && null == mods) {
							mods = new LinkedList<ModificationItem>();

							attr.remove(oldDN);
							attr.add(newDN);

							mods
									.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attr));
						}

						if (null == mods) {
							mods = new LinkedList<ModificationItem>();
							attr.remove(oldDN);

							// check whether we need to re-add the dummy member
							if (attr.size() == 0
									&& (ra.getCardinality() == Cardinality.ONE || ra
											.getCardinality() == Cardinality.ONE_OR_MANY))
								attr.add(directory.getDummyMember());

							mods
									.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attr));
						}
					}
				}

				if (null != mods) {
					final ModificationItem[] modsArray = mods
							.toArray(new ModificationItem[mods.size()]);

					DiropLogger.LOG.logModify(result.getName(), modsArray,
							"cascading update due to DN change of referenced object");

					ctx.modifyAttributes(result.getName(), modsArray);
				}
			}
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	SecondLevelCache getSecondLevelCache() {
		return secondLevelCache;
	}
}

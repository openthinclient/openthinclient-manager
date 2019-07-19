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

import org.openthinclient.common.directory.LDAPDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

/**
 * The TypeMapping handles all mapping related tasks for one mapped class.
 * 
 * @author levigo
 */
public class TypeMapping implements Cloneable {
	private static final Logger logger = LoggerFactory.getLogger(TypeMapping.class);

	public enum SearchScope {
		OBJECT(SearchControls.OBJECT_SCOPE), ONELEVEL(SearchControls.ONELEVEL_SCOPE), SUBTREE(
				SearchControls.SUBTREE_SCOPE);

		private final int sc;

		SearchScope(int sc) {
			this.sc = sc;
		}

		public int getScope() {
			return sc;
		}
	}

	/**
	 * The type's attributes.
	 */
	protected List<AttributeMapping> attributes = new ArrayList<AttributeMapping>();

	/**
	 * Other mapped attributes pointing back to this type. Not currently used.
	 */
	private List<AttributeMapping> referrers = new ArrayList<AttributeMapping>();

	/**
	 * The base RDN where objects of the type get stored by default. May be
	 * overridden by specifying a base RDN as an argument to the load/list/save
	 * methods.
	 */
	private final String baseRDN;

	/**
	 * This marker indicates an unchanged attribute, whatever the current value
	 * may be. Used by AttributeMappings to communicate an unchanged attribute
	 * during dehydration without having to know the current value.
	 */
	protected static final Object ATTRIBUTE_UNCHANGED_MARKER = "dc=6f70656e7468696e636c69656e74";

	/**
	 * The cached constructor for the type. Every mapped type must implement a
	 * public default constructor.
	 */
	private Constructor constructor;

	/**
	 * The parent Mapping.
	 */
	private Mapping mapping;

	/**
	 * The class of instances mapped by this mapping.
	 */
	private final Class modelClass;

	/**
	 * The key class of this type.
	 */
	private final String keyClass;

	/**
	 * The LDAP objectClasses used by elements of the type.
	 */
	// private final String objectClasses[];
	private String objectClasses[];

	/**
	 * The (Java-) attribute which holds the DN.
	 */
	private AttributeMapping dnAttribute;

	/**
	 * The (Java-) attribute which holds the RDN.
	 */
	private AttributeMapping rdnAttribute;

	/**
	 * The search filter used to search for objects of the mapped type below the
	 * base dn. May be overridden by specifying a filter as an argument to the
	 * load method.
	 */
	private final String searchFilter;

	/**
	 * Paged results control to receive search results in a controlled manner
	 * limited by the page size. AD defaults to 1000
	 */
	private final int PAGESIZE = 1000;

	/**
	 * The scope to use when listing objects of the mapped type.
	 * 
	 * @see SearchControls#setSearchScope(int)
	 */
	// private SearchScope defaultScope = SearchScope.ONELEVEL;
	private SearchScope defaultScope = SearchScope.SUBTREE;

	/**
	 * The connection descriptor to use for this mapping.
	 */
	private DirectoryFacade directoryFacade;

	private Name defaultBaseName;

	public TypeMapping(String className, String baseRDN, String searchFilter,
			String objectClasses, String keyClass) throws ClassNotFoundException {
		this.modelClass = Class.forName(className);
		this.baseRDN = baseRDN;
		this.searchFilter = searchFilter;
		this.objectClasses = null != objectClasses ? objectClasses
				.split("\\s*,\\s*") : new String[]{};
		this.keyClass = keyClass;
	}

	public List<AttributeMapping> getAttributeMappings() {
		return attributes;
	}

	/**
	 * @param mapping
	 * @throws NoSuchMethodException
	 */
	public void add(AttributeMapping attributeMapping) {
		attributeMapping.setTypeMapping(this);
		attributes.add(attributeMapping);
	}

	/**
	 * @return
	 * @throws DirectoryException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 */
	public Object create() throws DirectoryException {
		try {
			final Object instance = createInstance();

			rdnAttribute.initNewInstance(instance);
			for (final AttributeMapping am : attributes)
				am.initNewInstance(instance);

			return instance;
		} catch (final Exception e) {
			throw new DirectoryException("Can't create instance of " + modelClass);
		}
	}

	/**
	 * Create an empty object instance for this mapped type.
	 * 
	 * @return new, unhydrated instance
	 * @throws Exception
	 */
	private Object createInstance() throws Exception {
		final Constructor c = getConstructor();
		final Object newInstance = c.newInstance();

		return newInstance;
	}

	/**
	 * Create and hydrate an object instance from a set of attributes.
	 * 
	 * @param dn object's DN
	 * @param a attributes used to hydrate the object
	 * @param tx current transaction
	 * @return
	 * @throws Exception
	 */
	Object createInstanceFromAttributes(String dn, Attributes a, Transaction tx)
			throws Exception {
		final Object o = createInstance();
		setDN(dn, o);
		hydrateInstance(a, o, tx);

		return o;
	}

	/**
	 * Hydrate an object instance from a set of attributes.
	 * 
	 * @param a attributes used to hydrate the object
	 * @param o object to hydrate
	 * @param tx current transaction
	 * @throws DirectoryException
	 */
	private void hydrateInstance(Attributes a, Object o, Transaction tx)
			throws DirectoryException {
		// map RDN
		rdnAttribute.hydrate(o, a, tx);

		// map all other attributes
		for (final Object element : attributes) {
			final AttributeMapping am = (AttributeMapping) element;
			am.hydrate(o, a, tx);
		}
	}

	/**
	 * @param dn
	 * @param o
	 * @return
	 * @throws DirectoryException
	 */
	private Object setDN(String dn, Object o) throws DirectoryException {
		return dnAttribute.setValue(o, dn);
	}

	/**
	 * @return
	 */
	public String getBaseRDN() {
		return baseRDN;
	}

	/**
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	private Constructor getConstructor() throws SecurityException,
			NoSuchMethodException {
		if (null == constructor)
			constructor = modelClass.getConstructor();
		return constructor;
	}

	public Mapping getMapping() {
		return mapping;
	}

	/**
	 * @return
	 */
	public Class getMappedType() {
		return modelClass;
	}

	/**
	 * List object of the mapped type for the given base DN, search filter and
	 * scope.
	 * 
	 * @param filter the search filter
	 * @param searchBase the search base DN
	 * @param scope the search scope
	 * @param tx the current transaction
	 * @return
	 * @throws DirectoryException
	 */
	public Set list(Filter filter, String searchBase, SearchScope scope,
			Transaction tx) throws DirectoryException {
		try {
			final LdapContext ctx = directoryFacade.createDirContext();

			try {
				// construct filter. if filter is set, join this type's filter with
				// the supplied one.
				String applicableFilter = searchFilter;
				Object args[] = null;

				if (null != filter) {
					String parsedFilter = filter.getExpression(0);
					args = filter.getArgs();
					// enables filter regex for {0} like:
					// filter="(memberUid={0:uid=([^,]+)})"
					// FIXME: does not work for multiple regex appearances?
					final String parseFilter = ".*(\\{0:(.*)\\}).*";
					while (parsedFilter.matches(parseFilter)) {
						final Pattern getPattern = Pattern.compile(parseFilter);
						final Matcher getPatternMatcher = getPattern.matcher(parsedFilter);
						getPatternMatcher.find();
						final String patternString = getPatternMatcher.group(2);

						final Pattern pattern = Pattern.compile(patternString);
						final Matcher matcher = pattern.matcher(args[0].toString());
						matcher.find();

						parsedFilter = parsedFilter.replace(getPatternMatcher.group(1),
								matcher.group(1));
					}
					applicableFilter = "(&" + searchFilter + parsedFilter + ")";

					// FIXME: use Apache DS filter parser to properly upper-case the
					// search
					// expression

					// if (directoryFacade.guessDirectoryType()
					// .requiresUpperCaseRDNAttributeNames())
					// // ...
				}

				// the dn will frequently be a descendant of the ctx's name. If this
				// is the case, the prefix is removed, because search() expects
				// a relative name.
				if (null == searchBase)
					searchBase = null != baseRDN ? baseRDN : "";

				if (searchBase.equals("${basedn}"))
					searchBase = directoryFacade.fixNameCase(directoryFacade.getBaseDN());

				final Name searchBaseName = directoryFacade
						.makeRelativeName(searchBase);

				// we want our results to carry absolute names. This is where
				// they are rooted.
				final Name resultBaseName = directoryFacade
						.makeAbsoluteName(searchBase);

				if (logger.isDebugEnabled())
					logger.debug("listing objects of " + modelClass + " for base="
							+ searchBaseName + ", filter=" + filter);

				final SearchControls sc = new SearchControls();
				sc.setSearchScope(null != scope ? scope.getScope() : defaultScope
						.getScope());

				final Set results = new HashSet();
				try {
					NamingEnumeration<SearchResult> ne;

					DiropLogger.LOG.logSearch(searchBase, applicableFilter, args, sc,
							"list objects");

					// Activate paged results
					byte[] cookie = null;
					ctx.setRequestControls(new Control[]{new PagedResultsControl(
							PAGESIZE, Control.NONCRITICAL)});

					do {
						ne = ctx.search(searchBaseName, applicableFilter, args, sc);

						while (ne.hasMore()) {
							final SearchResult result = ne.next();

							Name elementName = directoryFacade.getNameParser().parse(
									result.getNameInNamespace());

							// FIX for A-DS bug: name isn't relative but should be.
							if (result.isRelative()
									&& !elementName.startsWith(resultBaseName))
								elementName = elementName.addAll(0, resultBaseName);

							// got it in the tx cache?
							Object instance = tx.getCacheEntry(elementName);
							if (null == instance) {
								final Attributes a = result.getAttributes();
								instance = createInstanceFromAttributes(elementName.toString(),
										a, tx);

								// cache the object
								tx.putCacheEntry(this, elementName, instance, a);
							}

							results.add(instance);
						}
						// Examine the paged results control response
						final Control[] controls = ctx.getResponseControls();
						if (controls != null)
							for (int i = 0; i < controls.length; i++)
								if (controls[i] instanceof PagedResultsResponseControl) {
									final PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
									cookie = prrc.getCookie();
								}
						// Re-activate paged results
						ctx.setRequestControls(new Control[]{new PagedResultsControl(
								PAGESIZE, cookie, Control.CRITICAL)});
					} while (cookie != null);

					// close the enumeration before cascading the load.
					ne.close();

					for (final Object o : results)
						for (final AttributeMapping am : attributes)
							// for (AttributeMapping am : attrs) {
							am.cascadePostLoad(o, tx);

				} catch (final NameNotFoundException e) {
					logger.warn("NameNotFoundException listing objects of " + modelClass
							+ " for base=" + searchBaseName
							+ ". Returning empty set instead.");
				}
				return results;

			} finally {
				ctx.close();
			}
		} catch (final Exception e) {
			throw new DirectoryException("Can't list objects for type " + modelClass,
					e);
		}
	}

	/**
	 * COPIED FROM LIST-METHOD ABOVE: CODE-DUPLICATION without Object-mapping
	 *
	 * @param filter the search filter
	 * @param searchBase the search base DN
	 * @param scope the search scope
	 * @param tx the current transaction
	 * @return found element names
	 * @throws DirectoryException
	 */
	public Set query(Filter filter, String searchBase, SearchScope scope, Transaction tx) throws DirectoryException {
		try {
			final LdapContext ctx = directoryFacade.createDirContext();

			try {
				// construct filter. if filter is set, join this type's filter with
				// the supplied one.
				String applicableFilter = searchFilter;
				Object args[] = null;

				if (null != filter) {
					String parsedFilter = filter.getExpression(0);
					args = filter.getArgs();
					// enables filter regex for {0} like:
					// filter="(memberUid={0:uid=([^,]+)})"
					// FIXME: does not work for multiple regex appearances?
					final String parseFilter = ".*(\\{0:(.*)\\}).*";
					while (parsedFilter.matches(parseFilter)) {
						final Pattern getPattern = Pattern.compile(parseFilter);
						final Matcher getPatternMatcher = getPattern.matcher(parsedFilter);
						getPatternMatcher.find();
						final String patternString = getPatternMatcher.group(2);

						final Pattern pattern = Pattern.compile(patternString);
						final Matcher matcher = pattern.matcher(args[0].toString());
						matcher.find();

						parsedFilter = parsedFilter.replace(getPatternMatcher.group(1),
								matcher.group(1));
					}
					applicableFilter = "(&" + searchFilter + parsedFilter + ")";
				}

				// the dn will frequently be a descendant of the ctx's name. If this
				// is the case, the prefix is removed, because search() expects
				// a relative name.
				if (null == searchBase)
					searchBase = null != baseRDN ? baseRDN : "";

				if (searchBase.equals("${basedn}"))
					searchBase = directoryFacade.fixNameCase(directoryFacade.getBaseDN());

				final Name searchBaseName = directoryFacade.makeRelativeName(searchBase);

				// we want our results to carry absolute names. This is where
				// they are rooted.
				final Name resultBaseName = directoryFacade.makeAbsoluteName(searchBase);

				if (logger.isDebugEnabled())
					logger.debug("listing objects of " + modelClass + " for base="
							+ searchBaseName + ", filter=" + filter);

				final SearchControls sc = new SearchControls();
				sc.setSearchScope(null != scope ? scope.getScope() : defaultScope
						.getScope());

				final Set results = new HashSet();
				try {

					DiropLogger.LOG.logSearch(searchBase, applicableFilter, args, sc,"query objects");
					NamingEnumeration<SearchResult> ne = ctx.search(searchBaseName, applicableFilter, args, sc);
					while (ne.hasMore()) {
						final SearchResult result = ne.next();

//						Name elementName = directoryFacade.getNameParser().parse(
//								result.getNameInNamespace());
//
//						// FIX for A-DS bug: name isn't relative but should be.
//						if (result.isRelative() && !elementName.startsWith(resultBaseName))
//							elementName = elementName.addAll(0, resultBaseName);

						results.add(result.getName().substring(result.getName().indexOf("=")+1));
					}

					// TODO: check -> close the enumeration before cascading the load.
					ne.close();

				} catch (final NameNotFoundException e) {
					logger.warn("NameNotFoundException query objects of " + modelClass
							+ " for base=" + searchBaseName
							+ ". Returning empty set instead.");
				}
				return results;

			} finally {
				ctx.close();
			}
		} catch (final Exception e) {
			throw new DirectoryException("Can't query objects for type " + modelClass,
					e);
		}
	}


	/**
	 * Load an object of the mapped type from the given DN.
	 * 
	 * @param tx the current transaction
	 * @param dn the object's DN
	 * @throws NamingException
	 * @throws DirectoryException
	 */
	public Object load(String dn, Transaction tx) throws DirectoryException {
		try {
			if (null == dn)
				dn = baseRDN;

			// make the dn absolute, even if it was relative.
			final DirContext ctx = tx.getContext(directoryFacade);

			final Name targetName = directoryFacade.makeAbsoluteName(dn);

			// got it in the tx cache?
			final Object cached = tx.getCacheEntry(targetName);
			if (null != cached)
				return cached;

			// seems like we've got to load it.
			if (logger.isDebugEnabled())
				logger.debug("loading object of " + modelClass + " for dn: "
						+ targetName);

			// FIXME: use lookup() instead of search
			final SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.OBJECT_SCOPE);

			Object o = null;

			// search() expects a base name relative to the ctx.
			final Name searchName = directoryFacade.makeRelativeName(dn);

			DiropLogger.LOG.logSearch(dn, searchFilter, null, sc,
					"loading single object");

			final NamingEnumeration<SearchResult> ne = ctx.search(searchName,
					searchFilter, null, sc);

			try {
				if (!ne.hasMore())
					throw new NameNotFoundException("No object for the given dn found.");

				final SearchResult result = ne.nextElement();

				if (ne.hasMore())
					// scope=OBJECT_SCOPE!
					throw new DirectoryException("More than one result return for query");

				final Attributes a = result.getAttributes();
				o = createInstanceFromAttributes(targetName.toString(), a, tx);

				tx.putCacheEntry(this, targetName, o, a);
			} finally {
				// close the enumeration before cascading the load.
				ne.close();
			}

			for (final AttributeMapping am : attributes)
				am.cascadePostLoad(o, tx);

			return o;
		} catch (final Exception e) {
			throw new DirectoryException("Can't load object", e);
		}
	}

	/**
	 * @param o
	 * @param transaction
	 * @param baseDN2
	 * @throws DirectoryException
	 */
	public void save(Object o, String baseDN, Transaction tx)
			throws DirectoryException {
		assert o.getClass().equals(modelClass);

		// break cycles
		if (tx.didAlreadyProcessEntity(o))
			return;
		tx.addEntity(o);

		try {
			final DirContext ctx = tx.getContext(directoryFacade);

			// if the object has already got a DN set, we update it. Otherwise
			// we save a new one.
			final String dn = getDN(o);
			Name name = null;
			if (null == dn)
				try {
					saveNewObject(o, ctx, baseDN, tx);

					return;
				} catch (final NameAlreadyBoundException e) {
					// The object's dn wasn't set. However, its
					// RDN may have pointed to an existing object.
					// Fall through to update mode.
					name = fillEmptyDN(o, ctx, baseDN);
					if (logger.isDebugEnabled())
						logger
								.debug("Caught NameAlreadyBoundException on saveNewObject for "
										+ name + ". trying update instead.");
				}

			// if the target name wasn't provided by the fall-through above,
			// build
			// it based on the object's dn attribute.
			// the dn will frequently be a descendant of the ctx's name. If this
			// is the case, the prefix is removed.
			if (null == name)
				name = directoryFacade.makeRelativeName(dn);

			try {
				DiropLogger.LOG.logGetAttributes(name, null, "save object");

				final Attributes currentAttributes = ctx.getAttributes(name);
				updateObject(o, ctx, name, currentAttributes, tx);
				return;
			} catch (final NameNotFoundException e) {
				throw new DirectoryException("Object to be updated no longer exists");
			}
		} catch (final DirectoryException e) {
			throw e;
		} catch (final Exception e) {
			throw new DirectoryException("Can't save object", e);
		}
	}

	/**
	 * @param o
	 * @param ctx
	 * @param baseDN
	 * @param tx
	 * @throws NamingException
	 * @throws DirectoryException
	 * @throws InvalidNameException
	 * @throws NamingException
	 * @throws DirectoryException
	 */
	private void saveNewObject(Object o, DirContext ctx, String baseDN,
			Transaction tx) throws DirectoryException,
			NamingException {
		final Name targetName = fillEmptyDN(o, ctx, baseDN);

		// perform cascading of stuff which has to be done before the new object
		// can been saved.
		for (final AttributeMapping attributeMapping : attributes)
			attributeMapping.cascadePreSave(o, tx);

		final BasicAttributes a = new BasicAttributes();
		rdnAttribute.dehydrate(o, a);

		fillAttributes(o, a);

		DiropLogger.LOG.logAdd(getDN(o), a, "save new object");
		ctx.bind(targetName, null, a);

		// cache new object
		tx.putCacheEntry(this, getDirectoryFacade().makeAbsoluteName(targetName),
				o, a);

		// perform cascading of stuff which has to be done after the new object
		// has been saved.
		try {
			for (final AttributeMapping attributeMapping : attributes)
				attributeMapping.cascadePostSave(o, tx, ctx);
		} catch (final DirectoryException t) {
			// rollback. FIXME: use transaction to do the dirty work
			try {
				DiropLogger.LOG.logDelete(targetName, "delete due to rollback");
				ctx.destroySubcontext(targetName);
			} catch (final Throwable u) {
				// ignore
			}
			throw t;
		}
	}

	/**
	 * @param o
	 * @param ctx
	 * @param baseDN
	 * @return
	 * @throws DirectoryException
	 * @throws NamingException
	 * @throws InvalidNameException
	 */
	private Name fillEmptyDN(Object o, DirContext ctx, String baseDN)
			throws DirectoryException, NamingException {
		// the dn will frequently be a descendant of the ctx's name. If this
		// is the case, the prefix is removed.

		if (null == baseDN)
			baseDN = this.baseRDN;

		if (null == baseDN && getDirectoryFacade().isReadOnly())
			baseDN = "";

		if (null == baseDN)
			throw new DirectoryException(
					"Can't save object: don't know where to save it to");

		final Name name = directoryFacade.makeRelativeName(baseDN);

		final Object rdnValue = rdnAttribute.getValue(o);
		if (null == rdnValue)
			throw new DirectoryException(
					"Can't save new instance: attribute for RDN (" + rdnAttribute
							+ ") not set.");

		// add rdn
		name.addAll(new LdapName(rdnAttribute.fieldName + "=" + rdnValue));

		setDN(directoryFacade.makeAbsoluteName(name).toString(), o);

		return name;
	}

	/**
	 * @param mapping
	 */
	void setMapping(Mapping mapping) {
		if (null != this.mapping)
			this.mapping.remove(this);

		this.mapping = mapping;
	}

	/**
	 * @param mapping
	 * @throws NoSuchMethodException
	 */
	public void setRDNAttribute(AttributeMapping rdnAttribute) {
		if (!dnAttribute.getFieldType().equals(String.class))
			throw new IllegalArgumentException(
					"The RDN Attribute must be of type string");

		rdnAttribute.setTypeMapping(this);
		this.rdnAttribute = rdnAttribute;
	}

	public AttributeMapping getRDNAttribute() {
		return this.rdnAttribute;
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "[TypeMapping class=" + modelClass + ", baseDN=" + baseRDN
				+ ", filter=" + searchFilter + "]";
	}

	/**
	 * @param o
	 * @param ctx
	 * @param targetName
	 * @param currentAttributes
	 * @param tx
	 * @throws DirectoryException
	 */
	private void updateObject(Object o, DirContext ctx, Name targetName,
			Attributes currentAttributes, Transaction tx) throws DirectoryException,
			NamingException {

		Name targetDN = getDirectoryFacade().makeAbsoluteName(targetName);
		tx.purgeCacheEntry(targetDN);

		try {
			final BasicAttributes newAttributes = new BasicAttributes();

			final Object rdn = rdnAttribute.dehydrate(o, newAttributes);
			if (null == rdn)
				throw new DirectoryException("Can't save new instance: "
						+ "attribute for RDN (" + rdnAttribute + ") not set.");

			if (!rdn.equals(currentAttributes.get(rdnAttribute.fieldName).get())
					&& LDAPDirectory.isMutable(o.getClass())) {
				// ok, go for a rename!
				targetName = renameObject(targetName, ctx, rdn, o, tx, newAttributes);

				// dn has changed as well...
				targetDN = getDirectoryFacade().makeAbsoluteName(targetName);
			}

			fillAttributes(o, newAttributes);

			final List<ModificationItem> mods = new LinkedList<ModificationItem>();

			// remove cleared Attributes
			if (currentAttributes.size() > 0) {
				final Attributes clearedAttributes = getClearedAttributes(
						(BasicAttributes) newAttributes.clone(),
						(Attributes) currentAttributes.clone());

				if (clearedAttributes.size() > 0) {
					final NamingEnumeration<Attribute> enmAttribute = (NamingEnumeration<Attribute>) clearedAttributes
							.getAll();

					while (enmAttribute.hasMore()) {
						final Attribute clearedAttribute = enmAttribute.next();
						mods.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE,
								clearedAttribute));

						if (logger.isDebugEnabled())
							logger.debug("The value of following Attribute will be cleared: "
									+ clearedAttribute);
					}
				}
			}

			// updates, adds
			final NamingEnumeration<Attribute> ne = newAttributes.getAll();
			try {
				while (ne.hasMore()) {
					final Attribute newValues = ne.next();

					final String id = newValues.getID();
					// FIXME: hitting apacheds bug if umlauts in cn attribute?
					// @see AttributeMapping.valueFromAttributes()
					//
					// better workaround would be to clone -> delete -> save clone
					// but for now: ignore cn attribute updates...
					if (id.equalsIgnoreCase("objectclass") || id.equalsIgnoreCase("cn")) {
						currentAttributes.remove(id);
						continue;
					}

					updateAttributes(currentAttributes, currentAttributes.get(id),
							newValues, mods, o);
				}
			} finally {
				ne.close();
			}

			// execute the modifications
			if (mods.size() > 0) {
				final ModificationItem mi[] = mods.toArray(new ModificationItem[mods
						.size()]);
				DiropLogger.LOG.logModify(targetName, mi, "update object");

				if (LDAPDirectory.isMutable(this.getMappedType()))
					ctx.modifyAttributes(targetName, mi);
			}

			tx.putCacheEntry(this, targetDN, o, newAttributes);

			// perform cascading of stuff which has to be done after the new
			// object has been saved.
			for (final AttributeMapping attributeMapping : attributes)
				attributeMapping.cascadePostSave(o, tx, ctx);
		} catch (final DirectoryException e) {
			throw e;
		} catch (final Throwable e) {
			throw new DirectoryException("Can't marshal instance of " + modelClass, e);
		}
	}

	private Name renameObject(Name oldName, DirContext ctx, Object rdn, Object o,
			Transaction tx, BasicAttributes attrib) throws NamingException,
			DirectoryException {
		final Name newName = oldName.getPrefix(oldName.size() - 1).add(
				rdnAttribute.fieldName + "=" + rdn);

		final String oldDN = getDN(o);
		final String newDN = ((Name) getDirectoryFacade().getBaseDNName().clone())
				.addAll(newName).toString();

		if (oldDN.equals(newDN))
			return oldName;

		DiropLogger.LOG.logModRDN(oldName, newName, "rename object");

		ctx.rename(oldName, newName);
		getMapping().updateReferences(tx, oldDN, newDN);

		// and tell the object about the new dn
		setDN(newDN, o);

		// perform cascading of stuff which has to be done after the
		// new object has been saved.
		for (final AttributeMapping attributeMapping : attributes)
			attributeMapping.cascadeRDNChange(o, oldDN, newDN, tx);

		// let the rdn attribute alone!
		attrib.remove(rdnAttribute.fieldName);

		return newName;
	}

	protected void updateAttributes(Attributes currentAttributes,
			Attribute currentValues, Attribute newValues,
			List<ModificationItem> mods, Object o) throws NamingException,
			DirectoryException {
		final String id = newValues.getID();
		if (currentValues != null) {
			// use identity comparison for unchanged marker!
			if (newValues.size() == 1
					&& newValues.get(0) == ATTRIBUTE_UNCHANGED_MARKER)
				currentAttributes.remove(id);
			else {
				if (!areAttributesEqual(newValues, currentValues))
					mods
							.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, newValues));
				currentAttributes.remove(id);
			}
		} else if (currentValues == null && newValues != null)
			mods.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, newValues));
	}

	private Attributes getClearedAttributes(BasicAttributes nowAttributes,
			Attributes ldapAttributes) throws NamingException {

		final Attributes cleared = new BasicAttributes();

		// ignore objectClasses
		nowAttributes.remove("objectClass");
		ldapAttributes.remove("objectClass");

		final NamingEnumeration<String> nowIDs = nowAttributes.getIDs();

		while (nowIDs.hasMore()) {
			final String id = nowIDs.next();
			ldapAttributes.remove(id);
		}

		final Set<String> attr = new HashSet<String>();

		for (final AttributeMapping am : attributes) {
			if (am instanceof ManyToManyMapping)
				continue;
			attr.add(am.fieldName);
		}

		final NamingEnumeration<String> ldapIDs = ldapAttributes.getIDs();

		while (ldapIDs.hasMore()) {
			final String id = ldapIDs.next();
			for (final String rightID : attr)
				if (rightID.equalsIgnoreCase(id))
					cleared.put(ldapAttributes.get(id));
		}

		return cleared;
	}

	/**
	 * @param a1
	 * @param a2
	 * @return
	 * @throws NamingException
	 */
	private boolean areAttributesEqual(Attribute a1, Attribute a2)
			throws NamingException {
		if (!a1.getID().equalsIgnoreCase(a2.getID()))
			return false;

		if (a1.get() == null && a2.get() == null)
			return true;

		if (a1.get() == null || a2.get() == null)
			return false;

		if (a1.size() != a2.size())
			return false;

		for (int i = 0; i < a1.size(); i++)
			if (a1.get() instanceof byte[])
				return Arrays.equals((byte[]) a1.get(), (byte[]) a2.get());
			else
				return a1.get(i).equals(a2.get(i));
		return true;
	}

	/**
	 * @param o
	 * @param a
	 * @throws DirectoryException
	 * @throws NamingException
	 */
	private void fillAttributes(Object o, BasicAttributes a)
			throws DirectoryException, NamingException {

		// map all other attributes
		for (final AttributeMapping attributeMapping : attributes)
			attributeMapping.dehydrate(o, a);

		// add object classes
		final Attribute objectClassesAttribute = new BasicAttribute("objectClass");
		for (final String oc : objectClasses)
			objectClassesAttribute.add(oc);
		a.put(objectClassesAttribute);
	}

	/**
	 * @param o
	 * @param transaction
	 * @return
	 * @throws DirectoryException
	 * @throws NamingException
	 */
	public boolean delete(Object o, Transaction tx) throws DirectoryException {
		if (!LDAPDirectory.isMutable(o.getClass()))
			return false;

		// break cycles
		if (tx.didAlreadyProcessEntity(o))
			return true;
		tx.addEntity(o);

		final String dn = getDN(o);
		if (null == dn)
			throw new DirectoryException(
					"Can't delete this object: no DN (mayby it wasn't saved before?)");
		try {
			final DirContext ctx = tx.getContext(directoryFacade);
			// checkMutable(ctx);
			// the dn will frequently be a descendant of the ctx's name. If
			// this
			// is the case, the prefix is removed.
			final Name targetName = directoryFacade.makeRelativeName(dn);

			// remove from cache
			tx.purgeCacheEntry(targetName);

			deleteRecursively(ctx, targetName, tx, "delete object");

			getMapping().updateReferences(tx, dn, null);

			try {
				// perform cascading of stuff which has to be done after the
				// object has been deleted.
				for (final AttributeMapping attributeMapping : attributes)
					attributeMapping.cascadeDelete(targetName, tx);
			} catch (final DirectoryException e) {
				logger.error("Exception during cascade post RDN change", e);
			}
			return true;

		} catch (final NameNotFoundException e) {
			logger.warn("Object to be deleted was not actually found.");
			return false;
		} catch (final Exception e) {
			throw new DirectoryException("Can't load object", e);
		}
	}

	/**
	 * @param ctx
	 * @param targetName
	 * @param tx
	 * @param comment TODO
	 * @throws NamingException
	 */
	static void deleteRecursively(DirContext ctx, Name targetName,
			Transaction tx, String comment) throws NamingException {

		final NamingEnumeration<NameClassPair> children = ctx.list(targetName);
		try {
			while (children.hasMore()) {
				final NameClassPair child = children.next();
				targetName.add(child.getName());
				deleteRecursively(ctx, targetName, tx, "delete recursively");
				targetName.remove(targetName.size() - 1);
			}
		} finally {
			children.close();
		}

		DiropLogger.LOG.logDelete(targetName, comment);
		try {
			ctx.destroySubcontext(targetName);
		} catch (final Exception e) {
		}

	}

	/**
	 * 
	 */
	public void initPostLoad() {
		for (final AttributeMapping am : attributes)
			am.initPostLoad();
	}

	/**
	 * @param mapping
	 */
	void addReferrer(AttributeMapping mapping) {
		referrers.add(mapping);
	}

	public AttributeMapping getDNAttribute() {
		return dnAttribute;
	}

	public void setDNAttribute(AttributeMapping dnAttribute) {
		if (!dnAttribute.getFieldType().equals(String.class))
			throw new IllegalArgumentException(
					"The DN Attribute must be of type string");

		this.dnAttribute = dnAttribute;
		dnAttribute.setTypeMapping(this);
	}

	/**
	 * @param o
	 * @return
	 * @throws DirectoryException
	 */
	String getDN(Object o) throws DirectoryException {
		return (String) dnAttribute.getValue(o);
	}

	public String[] getObjectClasses() {
		return objectClasses;
	}

	/**
	 * Refresh the given object's state from the directory.
	 * 
	 * @param o the object to refresh.
	 * @param tx the current transaction
	 * @throws DirectoryException
	 */
	public void refresh(Object o, Transaction tx) throws DirectoryException {
		try {
			final DirContext ctx = tx.getContext(directoryFacade);
			final String dn = getDN(o);
			try {
				final Name targetName = directoryFacade.makeRelativeName(dn);

				if (logger.isDebugEnabled())
					logger.debug("refreshing object of " + modelClass + " for dn: " + dn);

				DiropLogger.LOG.logGetAttributes(dn, null, "refresh object");

				final Attributes a = ctx.getAttributes(targetName);
				hydrateInstance(a, o, tx);

				for (final AttributeMapping am : attributes)
					am.cascadePostLoad(o, tx);

				// update object in cache, no matter what
				final Name absoluteName = directoryFacade.makeAbsoluteName(dn);
				tx.putCacheEntry(this, absoluteName, o, a);

			} catch (final NameNotFoundException n) {
				throw new DirectoryException("Can't refresh " + dn
						+ ": object doesn't exist (any longer?)");
			}
		} catch (final DirectoryException e) {
			throw e;
		} catch (final Exception e) {
			throw new DirectoryException("Can't refresh object", e);
		}
	}

	public void setScope(String scope) {
		this.defaultScope = SearchScope.valueOf(scope);
	}

	public void setScope(SearchScope scope) {
		this.defaultScope = scope;
	}

	/*
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected TypeMapping clone() throws CloneNotSupportedException {
		final TypeMapping clone = (TypeMapping) super.clone();

		clone.referrers = new ArrayList<AttributeMapping>();
		clone.attributes = new ArrayList<AttributeMapping>();

		for (final AttributeMapping am : attributes) {
			final AttributeMapping clonedAM = am.clone();
			clone.add(clonedAM);
		}

		return clone;
	}

	public String getSearchFilter() {
		return searchFilter;
	}

	public void setObjectClasses(String[] objectClasses) {
		this.objectClasses = objectClasses;
	}

	public String getKeyClass() {
		return keyClass;
	}

	void setDirectoryFacade(DirectoryFacade lcd) {
		this.directoryFacade = lcd;
	}

	DirectoryFacade getDirectoryFacade() {
		return directoryFacade;
	}

	public boolean matchesKeyClasses(Attribute objectClasses)
			throws NamingException {
		if (null == keyClass)
			return false;

		for (final NamingEnumeration<String> ne = (NamingEnumeration<String>) objectClasses
				.getAll(); ne.hasMore();)
			if (ne.next().equalsIgnoreCase(keyClass))
				return true;

		return false;
	}

	public Name getDefaultBaseName() throws NamingException {
		if (null == defaultBaseName) {
			final Name baseDNName = (Name) directoryFacade.getBaseDNName().clone();
			defaultBaseName = baseDNName.add(getBaseRDN());
		}

		return defaultBaseName;
	}

	protected void collectRefererAttributes(
			Set<ReferenceAttributeMapping> refererAttributes) {
		for (final AttributeMapping am : attributes)
			if (am instanceof ReferenceAttributeMapping)
				refererAttributes.add((ReferenceAttributeMapping) am);
	}

	/**
	 * Handle cases where the object's parent DN changed somewhere up the tree.
	 * 
	 * @param o the persistent object of which we need to up-date the name
	 * @param oldName the old name prefix
	 * @param newName the new name prefix
	 * @param tx the current transaction
	 * @throws DirectoryException
	 * @throws NamingException
	 */
	void handleParentNameChange(Object o, String oldDN, String newDN,
			Transaction tx) throws DirectoryException, NamingException {
		final String dn = getDN(o);

		// if the dn is null, the object is still transient and we don't have to
		// worry
		if (null != dn)
			if (dn.endsWith(oldDN))
				// update DN
				setDN(dn.substring(0, dn.length() - oldDN.length()) + newDN, o);
			else
				logger.warn("Unexpected state during parent DN change: object's dn "
						+ dn + " doesn't start with " + oldDN);

		// perform cascading of stuff which has to be done after the
		// new object has been saved.
		for (final AttributeMapping attributeMapping : attributes)
			attributeMapping.cascadeRDNChange(o, oldDN, newDN, tx);
	}
}

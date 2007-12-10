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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;
import org.openthinclient.common.directory.LDAPDirectory;

/**
 * The TypeMapping handles all mapping related tasks for one mapped class.
 * 
 * @author levigo
 */
public class TypeMapping implements Cloneable {
	private static final Logger logger = Logger.getLogger(TypeMapping.class);

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
	private List<AttributeMapping> attributes = new ArrayList<AttributeMapping>();

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

	protected static final Object ATTRIBUTE_UNCHANGED_MARKER = "unchanged";

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
		final Object newInstance = c.newInstance(new Object[]{});

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
	private Object createInstanceFromAttributes(String dn, Attributes a,
			Transaction tx) throws Exception {
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
			constructor = modelClass.getConstructor(new Class[]{});
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
			final DirContext ctx = tx.getContext(directoryFacade);

			// construct filter. if filter is set, join this type's filter with
			// the supplied one.
			String applicableFilter = searchFilter;
			Object args[] = null;

			if (null != filter) {
				applicableFilter = "(&" + searchFilter + filter.getExpression(0) + ")";
				args = filter.getArgs();

				if (directoryFacade.guessDirectoryType()
						.requiresUpperCaseRDNAttributeNames())
					for (int i = 0; args.length > i; i++)
						args[i] = Util.idToUpperCase(args[i].toString());
			}

			// the dn will frequently be a descendant of the ctx's name. If this
			// is the case, the prefix is removed, because search() expects
			// a base name relative to the ctx.
			if (null == searchBase)
				searchBase = null != baseRDN ? baseRDN : "";

			final Name searchBaseName = directoryFacade.makeRelativeName(searchBase);

			// we want or results to carry absolute names. This is where
			// they are rooted.
			final Name resultBaseName = directoryFacade.makeAbsoluteName(searchBase);

			if (logger.isTraceEnabled())
				logger.trace("listing objects of " + modelClass + " for base="
						+ searchBaseName + ", filter=" + filter);

			final SearchControls sc = new SearchControls();
			sc.setSearchScope(null != scope ? scope.getScope() : defaultScope
					.getScope());

			final Set results = new HashSet();
			try {
				NamingEnumeration<SearchResult> ne;

				ne = ctx.search(searchBaseName, applicableFilter, args, sc);

				try {
					while (ne.hasMore()) {
						final SearchResult result = ne.next();

						// we want an absolute element name. Unfortunately,
						// result.getNameInNamespace() is 1.5+ only, so we've
						// got to work this out ourselves.
						Name elementName = directoryFacade.getNameParser().parse(
								result.getName());

						// FIX for A-DS bug: name isn't relative but should be.
						if (result.isRelative() && !elementName.startsWith(resultBaseName))
							elementName = elementName.addAll(0, resultBaseName);

						// got it in the tx cache?
						Object instance = tx.getCacheEntry(elementName);
						if (null == instance) {
							instance = createInstanceFromAttributes(elementName.toString(),
									result.getAttributes(), tx);

							// cache the object
							tx.putCacheEntry(elementName, instance);
						}

						results.add(instance);
					}
				} finally {
					// close the enumeration before cascading the load.
					ne.close();
				}

				// FIXME: Michael: I don't see why the code below should be necessary.
				//
				// List<AttributeMapping> attrs = new ArrayList<AttributeMapping>();
				// NameParser np;
				// try {
				// np = ctx.getNameParser("");
				//
				// Name tmName = np.parse(ctx.getNameInNamespace());
				//
				// Map<Class, TypeMapping> tmMap =
				// mapping.getMappersByDirectory(tmName);
				//
				// if (null != tmMap) {
				// TypeMapping tm = tmMap.get(getMappedType());
				//
				// if (null != tm)
				// attrs = tm.attributes;
				// }
				//
				// } catch (NamingException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
				//
				// if (attrs.size() == 0)
				// attrs = attributes;

				for (final Object o : results)
					for (final AttributeMapping am : attributes)
						// for (AttributeMapping am : attrs) {
						am.cascadePostLoad(o, tx);

			} catch (final NameNotFoundException e) {
				logger.warn("NameNotFoundException listing objects of " + modelClass
						+ " for base=" + searchBaseName + ". Returning empty set instead.");
			}
			return results;

		} catch (final Exception e) {
			throw new DirectoryException("Can't list objects for type " + modelClass,
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
			if (logger.isTraceEnabled())
				logger.trace("loading object of " + modelClass + " for dn: "
						+ targetName);

			// FIXME: use lookup() instead of search
			final SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.OBJECT_SCOPE);

			Object o = null;

			// search() expects a base name relative to the ctx.
			final Name searchName = directoryFacade.makeRelativeName(dn);

			final NamingEnumeration<SearchResult> ne = ctx.search(searchName,
					searchFilter, null, sc);

			try {
				if (!ne.hasMore())
					throw new NameNotFoundException("No object for the given dn found.");

				final SearchResult result = ne.nextElement();

				if (ne.hasMore())
					// scope=OBJECT_SCOPE!
					throw new DirectoryException("More than one result return for query");

				o = createInstanceFromAttributes(targetName.toString(), result // load
						.getAttributes(), tx);

				tx.putCacheEntry(targetName, o);
			} finally {
				// close the enumeration before cascading the load.
				ne.close();
			}

			for (final AttributeMapping am : attributes)
				am.cascadePostLoad(o, tx);

			// cache the object
			tx.putCacheEntry(targetName, o);

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
					if (logger.isTraceEnabled())
						logger
								.trace("Caught NameAlreadyBoundException on saveNewObject for "
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
				final Attributes currentAttributes = ctx.getAttributes(name);
				updateObject(o, ctx, name, currentAttributes, tx);
				return;
			} catch (final NameNotFoundException e) {
				logger.error("Object to be updated no longer exists");
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
			Transaction tx) throws InvalidNameException, DirectoryException,
			NamingException {
		final Name targetName = fillEmptyDN(o, ctx, baseDN);

		// perform cascading of stuff which has to be done before the new object
		// can been saved.
		for (final AttributeMapping attributeMapping : attributes)
			attributeMapping.cascadePreSave(o, tx);

		final BasicAttributes a = new BasicAttributes();
		rdnAttribute.dehydrate(o, a);

		fillAttributes(o, a);

		if (logger.isDebugEnabled()) {
			logger.debug("ADD " + targetName);
			for (final NamingEnumeration<Attribute> e = a.getAll(); e.hasMore();)
				logger.debug("    " + e.next());
		}

		ctx.bind(targetName, null, a);
		// perform cascading of stuff which has to be done after the new object
		// has been saved.
		try {
			for (final AttributeMapping attributeMapping : attributes)
				attributeMapping.cascadePostSave(o, tx, ctx);
		} catch (final DirectoryException t) {
			// rollback
			try {
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
			throws DirectoryException, NamingException, InvalidNameException {
		// the dn will frequently be a descendant of the ctx's name. If this
		// is the case, the prefix is removed.

		if (null == baseDN)
			baseDN = this.baseRDN;

		if (null == baseDN && !LDAPDirectory.isMutable(getMappedType()))
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
		name.addAll(directoryFacade.getNameParser().parse(
				rdnAttribute.fieldName + "=" + rdnValue));

		// and tell the object about it (the full absolute dn!)
		Name baseDNName = directoryFacade.getBaseDNName();

		// make copy lest we don't mess up the LCDs name.
		baseDNName = (Name) baseDNName.clone();

		setDN(baseDNName.addAll(name).toString(), o);

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
			AttributeInUseException {

		// clear cache FIXME: this name may be relative!
		tx.purgeCacheEntry(targetName);
		try {
			final BasicAttributes attrib = new BasicAttributes();

			final Object rdn = rdnAttribute.dehydrate(o, attrib);
			if (null == rdn)
				throw new DirectoryException("Can't save new instance: "
						+ "attribute for RDN (" + rdnAttribute + ") not set.");

			if (!rdn.equals(currentAttributes.get(rdnAttribute.fieldName).get()))
				// ok, go for a rename!
				targetName = renameObjects(targetName, ctx, rdn, o, tx, attrib);

			fillAttributes(o, attrib);

			final List<ModificationItem> mods = new LinkedList<ModificationItem>();

			// remove cleared Attributes
			if (currentAttributes.size() > 0) {
				final Attributes clearedAttributes = getClearedAttributes(
						(BasicAttributes) attrib.clone(), (Attributes) currentAttributes
								.clone());

				if (clearedAttributes.size() > 0) {
					final NamingEnumeration<Attribute> enmAttribute = (NamingEnumeration<Attribute>) clearedAttributes
							.getAll();

					while (enmAttribute.hasMore()) {
						final Attribute clearedAttribute = enmAttribute.next();
						mods.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE,
								clearedAttribute));

						if (logger.isTraceEnabled())
							logger.trace("The value of following Attribute will be cleared: "
									+ clearedAttribute);
					}
				}
			}

			// updates, adds
			final NamingEnumeration<Attribute> ne = attrib.getAll();
			try {
				while (ne.hasMore()) {
					final Attribute a = ne.next();

					final String id = a.getID();
					if (id.equalsIgnoreCase("objectclass")) {
						currentAttributes.remove(id);
						continue;
					}

					final Attribute currentAttribute = currentAttributes.get(id);

					updateAttributes(currentAttributes, currentAttribute, a, mods, o);
				}
			} finally {
				ne.close();
			}

			// execute the modifications
			if (mods.size() > 0) {
				if (logger.isDebugEnabled()) {
					if (logger.isDebugEnabled())
						logger.debug("UPDATE " + targetName);
					for (final ModificationItem mi : mods)
						logger.debug("   - " + mi);
				}

				final ModificationItem mi[] = mods.toArray(new ModificationItem[mods
						.size()]);

				if (LDAPDirectory.isMutable(this.getMappedType()))
					ctx.modifyAttributes(targetName, mi);
			}

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

	private Name renameObjects(Name targetName, DirContext ctx, Object rdn,
			Object o, Transaction tx, BasicAttributes attrib) throws NamingException,
			DirectoryException {
		final Name newName = targetName.getPrefix(targetName.size() - 1).add(
				rdnAttribute.fieldName + "=" + rdn);
		final Name ctxName = getDirectoryFacade().getBaseDNName();

		final String oldDN = getDN(o);
		final String newDN = ctxName.addAll(newName).toString();

		if (logger.isDebugEnabled())
			logger.debug("RENAME: " + targetName + " -> " + newName);

		ctx.rename(targetName, newName);
		getMapping().updateReferences(tx, oldDN, newDN);

		targetName = newName;
		// and tell the object about the new dn
		setDN(newDN, o);

		try {
			// perform cascading of stuff which has to be done after the
			// new object has been saved.
			for (final AttributeMapping attributeMapping : attributes)
				attributeMapping.cascadeRDNChange(targetName, newName);
		} catch (final DirectoryException e) {
			logger.error("Exception during cascade post RDN change", e);
		}

		// let the rdn attribute alone!
		attrib.remove(rdnAttribute.fieldName);

		return targetName;
	}

	protected void updateAttributes(Attributes currentAttributes,
			Attribute currentValue, Attribute newValue, List<ModificationItem> mods,
			Object o) throws NamingException, DirectoryException {
		final String id = newValue.getID();
		if (currentValue != null) {
			// use identity comparison for unchanged marker!
			if (newValue.size() == 1 && newValue.get(0) == ATTRIBUTE_UNCHANGED_MARKER)
				currentAttributes.remove(id);
			else {
				if (!areAttributesEqual(newValue, currentValue))
					mods
							.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, newValue));
				currentAttributes.remove(id);
			}
		} else if (currentValue == null && newValue != null)
			mods.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, newValue));
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
	 */
	private void fillAttributes(Object o, BasicAttributes a)
			throws DirectoryException {

		// map all other attributes
		for (final AttributeMapping attributeMapping : attributes)
			attributeMapping.dehydrate(o, a); // there are different dehydrate !!!

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

			getMapping().updateReferences(tx, dn, null);
			deleteRecursively(ctx, targetName, tx);

			try {
				// perform cascading of stuff which has to be done after the
				// new
				// object
				// has been saved.
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
	 * @throws NamingException
	 */
	static void deleteRecursively(DirContext ctx, Name targetName, Transaction tx)
			throws NamingException {

		final NamingEnumeration<NameClassPair> children = ctx.list(targetName);
		try {
			while (children.hasMore()) {
				final NameClassPair child = children.next();
				targetName.add(child.getName());
				deleteRecursively(ctx, targetName, tx);
				targetName.remove(targetName.size() - 1);
			}
		} finally {
			children.close();
		}

		if (logger.isDebugEnabled())
			logger.debug("DELETE: " + targetName);
		try {
			ctx.destroySubcontext(targetName);
		} catch (final Exception e) {
		}

	}

	// FIXME: type-specific stuff is bad! Merge renameLocality with rename
	// use-case
	// of clearReferences.
	// private void renameLocality(Transaction tx, String dnMember, String
	// newName)
	// throws NamingException, DirectoryException {
	//
	// final DirContext ctx = tx.getContext(Client.class);
	//
	// final Class[] classes = new Class[]{Client.class};
	//
	// final Set<DirectoryObject> set = new HashSet<DirectoryObject>();
	//
	// for (final Class cl : classes) {
	// final Set<DirectoryObject> list = getMapping().list(cl);
	// set.addAll(list);
	// }
	//
	// for (final DirectoryObject o : set) {
	// final Name targetName = Util.makeRelativeName(getDN(o), ctx);
	//
	// final Attributes attrs = ctx.getAttributes(targetName);
	//
	// final Attribute a = attrs.get("l");
	//
	// if (a != null)
	// for (int i = 0; a.size() > i; i++)
	// if (a.get(i).equals(dnMember)) {
	//
	// if (a.size() == 0) {
	// final String dummy = OneToManyMapping.getDUMMY_MEMBER();
	// a.add(dummy);
	// }
	//
	// if (!newName.equals("")) {
	// a.remove(i);
	// String mod = "";
	// if (newName.startsWith("L"))
	// mod = Util.idToUpperCase(newName) + ","
	// + ctx.getNameInNamespace();
	//
	// if (newName.startsWith("l"))
	// mod = Util.idToLowerCase(newName) + ","
	// + ctx.getNameInNamespace();
	// a.add(mod);
	// }
	//
	// final ModificationItem[] mods = new ModificationItem[1];
	// mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, a);
	//
	// ctx.modifyAttributes(targetName, mods);
	// }
	// }
	// }

	/**
	 * 
	 */
	protected void initPostLoad() {
		for (final AttributeMapping am : attributes)
			am.initPostLoad();
	}

	/**
	 * @param mapping2
	 */
	public void addReferrer(AttributeMapping mapping) {
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

				if (logger.isTraceEnabled())
					logger.trace("refreshing object of " + modelClass + " for dn: " + dn);

				hydrateInstance(ctx.getAttributes(targetName), o, tx);

				for (final AttributeMapping am : attributes)
					am.cascadePostLoad(o, tx);

				// update object in cache, no matter what
				final Name absoluteName = directoryFacade.makeAbsoluteName(dn);
				tx.putCacheEntry(absoluteName, o);

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

	public Name getDefaultBaseName() throws InvalidNameException, NamingException {
		if (null == defaultBaseName) {
			final Name baseDNName = (Name) directoryFacade.getBaseDNName().clone();
			defaultBaseName = baseDNName.add(getBaseRDN());
		}

		return defaultBaseName;
	}

	void ensureDNSet(Object child, String baseDN, Transaction tx)
			throws DirectoryException {
		if (null == getDN(child))
			try {
				fillEmptyDN(child, tx.getContext(getDirectoryFacade()), baseDN);
			} catch (final Exception e) {
				throw new DirectoryException("Can't fill DN", e);
			}
	}

	protected void collectRefererAttributes(Set<String> refererAttributes) {
		for (final AttributeMapping am : attributes)
			am.collectRefererAttributes(refererAttributes);
	}
}

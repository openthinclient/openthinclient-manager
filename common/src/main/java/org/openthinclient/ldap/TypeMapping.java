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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
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
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Group;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.UserGroup;


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
	 * The base DN where objects of the type get stored by default. May be
	 * overridden by specifying a base DN as an argument to the save method.
	 */
	private String baseDN;

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
	private Class modelClass;
	
	/**
	 * The key class of this type.
	 */
	private String keyClass;

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
	// private final String searchFilter;
	private String searchFilter;
	/**
	 * The scope to use when listing objects of the mapped type.
	 * 
	 * @see SearchControls#setSearchScope(int)
	 */
	// private SearchScope defaultScope = SearchScope.ONELEVEL;
	private SearchScope defaultScope = SearchScope.SUBTREE;

	/**
	 * The flag which are set by the xml mappings.
	 */
	private final String canUpdate; // need ????
	
	/**
	 * The flag which are set by openRealm in LDAPDirectory.
	 */
	private boolean mutable = true;

	/**
	 * The flag if it is a creation of a new object
	 */
	private static boolean isNewAction;

	/**
	 * Lists with uniqueMembers which must delete
	 */
	private static ArrayList<DirectoryObject> toDelete = new ArrayList<DirectoryObject>();
	/**
	 * Lists with uniqueMembers which must create
	 */
	private static ArrayList<DirectoryObject> toMakeNew = new ArrayList<DirectoryObject>();

	/**
	 * the object
	 */
	private static DirectoryObject currentObject;
	/**
	 * Name of the object
	 */
	private static String currentObjectName;

	public TypeMapping(String className, String baseDN, String searchFilter,
			String objectClasses, String canUpdate, String keyClass)
			throws ClassNotFoundException {
		this.modelClass = Class.forName(className);
		this.baseDN = baseDN;
		this.searchFilter = searchFilter;
		this.objectClasses = null != objectClasses ? objectClasses
				.split("\\s*,\\s*") : new String[]{};
		this.canUpdate = canUpdate;
		this.keyClass = keyClass;
		setCannotUpdate(className, baseDN, searchFilter, objectClasses, canUpdate);
	}

	/**
	 * ???????????????????????????????????????????????????????????
	 * 
	 * @param className
	 * @param baseDN
	 * @param searchFilter
	 * @param objectClasses
	 * @param canUpdate
	 */

	private void setCannotUpdate(String className, String baseDN,
			String searchFilter, String objectClasses, String canUpdate) {

		boolean notMutable = canUpdate.equalsIgnoreCase("false");
		boolean isUser = className
				.equalsIgnoreCase("org.openthinclient.common.model.User");
		boolean isGroup = className
				.equalsIgnoreCase("org.openthinclient.common.model.UserGroup");

		if (isUser && notMutable) {
			DirectoryObject.UserIsReadOnly = true;
		} else if (isUser && (notMutable == false)) {
			DirectoryObject.UserIsReadOnly = false;
		}

		if (isGroup && notMutable) {
			DirectoryObject.GroupIsReadOnly = true;
		} else if (isGroup && (notMutable == false)) {
			DirectoryObject.GroupIsReadOnly = false;
		}
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
			Object instance = createInstance();

			rdnAttribute.initNewInstance(instance);
			for (AttributeMapping am : attributes) {
				am.initNewInstance(instance);
			}

			return instance;
		} catch (Exception e) {
			throw new DirectoryException("Can't create instance of " + modelClass);
		}
	}

	/**
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IllegalArgumentException
	 */
	private Object createInstance() throws SecurityException,
			NoSuchMethodException, IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		Constructor c = getConstructor();
		Object newInstance = c.newInstance(new Object[]{});
		return newInstance;
	}

	/**
	 * @param dn
	 * @param a
	 * @param tx TODO
	 * @return
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NamingException
	 */
	private Object createInstanceFromAttributes(String dn, Attributes a,
			Transaction tx) throws Exception {
		// create new instance
		Object o;
		o = createInstance();
		setDN(dn, o);

		hydrateInstance(a, o, tx);
		return o;
	}

	/**
	 * @param a
	 * @param o
	 * @param tx TODO
	 * @throws DirectoryException
	 */
	private void hydrateInstance(Attributes a, Object o, Transaction tx)
			throws DirectoryException {
		// map RDN
		rdnAttribute.hydrate(o, a, tx);

		// map all other attributes
		for (Iterator i = attributes.iterator(); i.hasNext();) {
			AttributeMapping am = (AttributeMapping) i.next();
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
	public String getBaseDN() {
		return baseDN;
	}

	/**
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	private Constructor getConstructor() throws SecurityException,
			NoSuchMethodException {
		if (null == constructor) {
			constructor = modelClass.getConstructor(new Class[]{});
		}
		return constructor;
	}

	public Mapping getMapping() {
		return mapping;
	}

	/**
	 * @return
	 */
	public Class getModelClass() {
		return modelClass;
	}

	/**
	 * @param filter
	 * @param searchBase
	 * @param scope TODO
	 * @param tx TODO
	 * @param directory
	 * @return
	 * @throws DirectoryException
	 */
	public Set list(Filter filter, String searchBase, SearchScope scope,
			Transaction tx) throws DirectoryException {
		try {
			
			DirContext ctx = null;
			
			// determine whether the object DN points to an absolute directory
			if (null != searchBase)
				ctx = tx.findContextByDN(searchBase);

			// otherwise we're fine with the default directory for the model class
			if (null == ctx)
				ctx = tx.getContext(modelClass);

			// construct filter. if filter is set, join this type's filter with
			// the
			// supplied one.
			String applicableFilter = searchFilter;
			Object args[] = null;

			if (null != filter) {
				applicableFilter = "(&" + searchFilter + filter.getExpression(0) + ")";
				args = filter.getArgs();
			}
			if (args != null) {
				for (int i = 0; args.length > i; i++) {
					args[i] = idToUpperCase(args[i].toString());
				}
			}
			// the dn will frequently be a descendant of the ctx's name. If this
			// is the case, the prefix is removed, because search() expects
			// a base name relative to the ctx.
			if (null == searchBase) {
				searchBase = null != baseDN ? baseDN : "";
			}

			Name searchBaseName = makeRelativeName(searchBase, ctx);
			// we want or results to carry absolute names. This is where
			// they are rooted.
			Name resultBaseName = makeAbsoluteName(searchBase, ctx);
			if (logger.isDebugEnabled()) {
				logger.debug("listing objects of " + modelClass + " for base="
						+ searchBaseName + ", filter=" + filter);
			}

			SearchControls sc = new SearchControls();
			sc.setSearchScope(null != scope ? scope.getScope() : defaultScope
					.getScope());

			Set results = new HashSet();
			try {
				NamingEnumeration<SearchResult> ne;
				NameParser nameParser;
				
				ne = ctx.search(searchBaseName, applicableFilter, args, sc);

				nameParser = ctx.getNameParser("");

				try {
					while (ne.hasMore()) {
						SearchResult result = ne.next();

						// we want an absolute element name. Unfortunately,
						// result.getNameInNamespace() is 1.5+ only, so we've
						// got to work this out ourselves.
						Name elementName = nameParser.parse(result.getName());

						// FIX for A-DS bug: name isn't relative but should be.
						if (result.isRelative() && !elementName.startsWith(resultBaseName)) {
							elementName = elementName.addAll(0, resultBaseName);
						}

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
				List<AttributeMapping> attrs = new ArrayList<AttributeMapping>();
				NameParser np;
				try {
					np = ctx.getNameParser("");

					Name tmName = np.parse(ctx.getNameInNamespace());

					Mapping map = tx.getMapping();

					Map<Class, TypeMapping> tmMap = map.getMappersByDirectory(tmName);

					if (null != tmMap) {
						TypeMapping tm = tmMap.get(getModelClass());

						if (null != tm)
							attrs = tm.attributes;
						}

				} catch (NamingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if(attrs.size() == 0)
					attrs = attributes;

				for (Object o : results) {

//					for (AttributeMapping am : attributes) {
					for (AttributeMapping am : attrs) {
					
						am.cascadePostLoad(o, tx);
					}
				}

			} catch (NameNotFoundException e) {
				logger.warn("NameNotFoundException listing objects of " + modelClass
						+ " for base=" + searchBaseName + ". Returning empty set instead.");
			}
			return results;

		} catch (Exception e) {
			throw new DirectoryException("Can't list objects for type " + modelClass,
					e);
		}
	}

	/**
	 * @param tx TODO
	 * @param string
	 * @throws NamingException
	 * @throws DirectoryException
	 */
	public Object load(String dn, Transaction tx) throws DirectoryException {
		try {
			if (null == dn) {
				dn = baseDN;
			}
			// make the dn absolute, even if it was relative.
			DirContext ctx = null;
			String tmpDN = TypeMapping.idToLowerCase(dn);
			
			if (null != tmpDN)
				ctx = tx.findContextByDN(tmpDN);
			
			if(null == ctx)
				ctx = tx.getContext(modelClass);
				
			dn = idToLowerCase(dn);

			Name targetName = makeAbsoluteName(dn, ctx);

			// got it in the tx cache?
			Object cached = tx.getCacheEntry(targetName);
			if (null != cached) {
				return cached;
			}

			// seems like we've got to load it.
			if (logger.isDebugEnabled()) {
				logger.debug("loading object of " + modelClass + " for dn: "
						+ targetName);
			}

			// FIXME: use lookup() instead of search
			SearchControls sc = new SearchControls();
			sc.setSearchScope(SearchControls.OBJECT_SCOPE);

			Object o = null;
			// search() expects a base name relative to the ctx.
			Name searchName = makeRelativeName(dn, ctx);
			
			NamingEnumeration<SearchResult> ne = ctx.search(searchName, searchFilter,
					null, sc);

			try {
				if (!ne.hasMore()) {
					throw new NameNotFoundException("No object for the given dn found.");
				}

				SearchResult result = ne.nextElement();

				if (ne.hasMore()) {
					// scope=OBJECT_SCOPE!
					throw new DirectoryException("More than one result return for query");
				}
				o = createInstanceFromAttributes(targetName.toString(), result // load
						.getAttributes(), tx);

				tx.putCacheEntry(targetName, o);
			} finally {
				// close the enumeration before cascading the load.
				ne.close();
			}

			for (AttributeMapping am : attributes) {
				am.cascadePostLoad(o, tx);
			}

			// cache the object
			tx.putCacheEntry(targetName, o);

			return o;
		} catch (Exception e) {
			throw new DirectoryException("Can't load object", e);
		}
	}

	/**
	 * @param dn
	 * @param ctx
	 * @return
	 * @throws NamingException
	 */
	public static Name makeRelativeName(String dn, DirContext ctx)
			throws NamingException {
		// // FIXME: cache name parser
		if ((dn.length() > 0) && dn.endsWith(ctx.getNameInNamespace())
				&& !dn.equalsIgnoreCase(ctx.getNameInNamespace())) {
			dn = dn.substring(0, dn.length() - ctx.getNameInNamespace().length() - 1);
		}
		return ctx.getNameParser("").parse(dn);
	}

	/**
	 * @param dn
	 * @param ctx
	 * @return
	 * @throws NamingException
	 */
	static Name makeAbsoluteName(String dn, DirContext ctx)
			throws NamingException {
		if (!dn.endsWith(ctx.getNameInNamespace())) {
			if (dn.length() > 0) {
				dn = dn + "," + ctx.getNameInNamespace();
			} else {
				dn = ctx.getNameInNamespace();
			}
		}
		return ctx.getNameParser("").parse(dn);
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
		if (tx.didAlreadyProcessEntity(o)) {
			return;
		}
		tx.addEntity(o);

		try {
			DirContext ctx = null;

			// if the object has already got a DN set, we update it. Otherwise
			// we save a new one.
			String dn = getDN(o);

			// determine whether the object DN points to an absolute directory
			if (null != dn)
				ctx = tx.findContextByDN(dn);

			// determine whether the base DN points to an absolute directory
			if (null != baseDN)
				ctx = tx.findContextByDN(baseDN);

			// otherwise we're fine with the default directory for the model class
			if (null == ctx)
				ctx = tx.getContext(modelClass);

			Name targetName = null;

			if (null == dn) {
				try {
					saveNewObject(o, ctx, baseDN, tx);

					return;
				} catch (NameAlreadyBoundException e) {
					// fall through to update
					targetName = fillEmptyDN(o, ctx, baseDN);
					if (logger.isDebugEnabled()) {
						logger
								.debug("Caught NameAlreadyBoundException on saveNewObject for "
										+ targetName + ". trying update instead.");
					}
				}
			}

			// if the target name wasn't provided by the fall-through above,
			// build
			// it based on the object's dn attribute.
			// the dn will frequently be a descendant of the ctx's name. If this
			// is the case, the prefix is removed.
			if (null == targetName) {
				targetName = makeRelativeName(dn, ctx);
			}

			try {

				Attributes currentAttributes = ctx.getAttributes(targetName);
				updateObject(o, ctx, targetName, currentAttributes, tx);
				return;
			} catch (NameNotFoundException e) {
				// fall through
				logger.warn("???");
			}
		} catch (DirectoryException e) {
			throw e;
		} catch (Exception e) {
			throw new DirectoryException("Can't save object", e);
		}
	}

	/**
	 * @param ctx
	 * @throws NamingException
	 * @throws DirectoryException
	 */
	private void checkMutable(DirContext ctx) throws NamingException,
			DirectoryException {
		Object mutable = ctx.getEnvironment().get(Mapping.MAPPING_IS_MUTABLE);
		if ((null != mutable) && mutable.toString().equalsIgnoreCase("false")) {
			throw new DirectoryException("Objects of type " + modelClass
					+ " can't be modified");
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
		Name targetName = fillEmptyDN(o, ctx, baseDN);

		// perform cascading of stuff which has to be done before the new object
		// can been saved.
		for (Iterator<AttributeMapping> i = attributes.iterator(); i.hasNext();) {
			i.next().cascadePreSave(o, tx);
		}

		BasicAttributes a = new BasicAttributes();
		rdnAttribute.dehydrate(o, a);

		fillAttributes(o, a);
		ctx.bind(targetName, null, a);
		// perform cascading of stuff which has to be done after the new object
		// has been saved.
		try {
			for (Iterator<AttributeMapping> i = attributes.iterator(); i.hasNext();) {
				i.next().cascadePostSave(o, tx, ctx);
			}
		} catch (DirectoryException t) {
			// rollback
			try {
				ctx.destroySubcontext(targetName);
			} catch (Throwable u) {
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

		if (null == baseDN) {
			baseDN = this.baseDN;
		}

		if ((null == baseDN) && !LDAPDirectory.isMutable(getModelClass())) {
			baseDN = "";
		}

		if (null == baseDN) {
			throw new DirectoryException(
					"Can't save object: don't know where to save it to");
		}

		Name ctxName = ctx.getNameParser("").parse(ctx.getNameInNamespace());
		Name targetName = ctx.getNameParser("").parse(baseDN);
		if (targetName.startsWith(ctxName)) {
			targetName = targetName.getSuffix(ctxName.size());
		}

		Object rdnValue = rdnAttribute.getValue(o);
		if (null == rdnValue) {
			throw new DirectoryException(
					"Can't save new instance: attribute for RDN (" + rdnAttribute
							+ ") not set.");
		}

		// add rdn
		targetName.addAll(ctx.getNameParser("").parse(
				rdnAttribute.fieldName + "=" + rdnValue));

		// and tell the object about it (the full absolute dn!)
		setDN(ctxName.addAll(targetName).toString(), o);
		return targetName;
	}

	/**
	 * @param mapping
	 */
	void setMapping(Mapping mapping) {
		this.mapping = mapping;
	}

	/**
	 * @param mapping
	 * @throws NoSuchMethodException
	 */
	public void setRDNAttribute(AttributeMapping rdnAttribute) {
		if (!dnAttribute.getFieldType().equals(String.class)) {
			throw new IllegalArgumentException(
					"The RDN Attribute must be of type string");
		}

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
		return "[TypeMapping class=" + modelClass + ", baseDN=" + baseDN
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


		if (logger.isDebugEnabled()) {
			logger.debug("updateObject(): object=" + o + ", ctx=" + ctx
					+ ", targetName=" + targetName + " attributes=" + currentAttributes);
		}

		// clear cache
		tx.purgeCacheEntry(targetName);
		try {
			BasicAttributes attrib = new BasicAttributes();

			Object rdn = rdnAttribute.dehydrate(o, attrib);
			if (null == rdn) {
				throw new DirectoryException("Can't save new instance: "
						+ "attribute for RDN (" + rdnAttribute + ") not set.");
			}

			if (!rdn.equals(currentAttributes.get(rdnAttribute.fieldName).get())) {
				// ok, go for a rename!
				renameObjects(targetName, ctx, rdn, o, tx, attrib);
			}

			fillAttributes(o, attrib);
			
			List<ModificationItem> mods = new LinkedList<ModificationItem>();

			//remove cleared Attributes
//			if(currentAttributes.size() > 0){
//				Attributes clearedAttributes = getClearedAttributes((BasicAttributes)attrib.clone(), (Attributes) currentAttributes.clone());
//				
//				if(clearedAttributes.size() > 0){
//					NamingEnumeration<Attribute> enmAttribute = (NamingEnumeration<Attribute>) clearedAttributes.getAll();
//					
//					while(enmAttribute.hasMore()){
//						Attribute clearedAttribute = enmAttribute.next();
////							mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, clearedAttribute));
//							mods.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, clearedAttribute));
//							
//
//						if (logger.isDebugEnabled()) {
//							logger.debug("The value of following Attribute will be cleared: " + clearedAttribute);
////							System.out.println("The value of following Attribute will be cleared: " + clearedAttribute);
//						}
//					}
//				}
//			}

		
			// updates, adds
			NamingEnumeration<Attribute> ne = attrib.getAll();
			try {
				while (ne.hasMore()) {
					Attribute a = ne.next();

					if (a.toString().startsWith("uniqueMember")) {
						if (logger.isDebugEnabled()) {
							logger.debug("Attribute a: " + a);
						}

					}

					String id = a.getID();
					if (logger.isDebugEnabled()) {
						logger.debug("id: " + id);
					}
					// never update the objectclass attribute
					if (id.equalsIgnoreCase("objectclass")) {
						currentAttributes.remove(id);
						continue;
					}

					Attribute currentAttribute = currentAttributes.get(id);

					if (logger.isDebugEnabled()) {
						logger.debug("currentAttribute: " + currentAttribute);
					}

					boolean isMember;
					if (a.toString().startsWith("uniquemember")
							|| a.toString().startsWith("uniqueMember")
							|| a.toString().startsWith("member")
							|| a.toString().startsWith("memberOf")) {
						isMember = true;
					} else {
						isMember = false;
					}

					// not use for the uniqueMembers
					if ((isMember == false) && (getIsNewAction() == false)) {
						updateNormalObjects(currentAttribute, currentAttributes, a, mods, id);
					}

					// use for the uniqueMembers
					if (isMember == true
							&& getIsNewAction() == false
							&& (rdn.toString().equals(getCurrentObjectName()) || getCurrentObjectName()
									.toString().equals("RealmConfiguration")) // to modify
					// the
					// administrators*/
					/* && isDummy == false */) {
						updateMember(currentAttribute, currentAttributes, a, mods, id, o, ctx, tx);
		
					}
				}
			} finally {
				ne.close();
			}

			if (logger.isDebugEnabled()) {
				for (ModificationItem mi : mods) {
					logger.debug("modification: " + mi);
				}
			}

			// execute the modifications
			if (mods.size() > 0) {
				ModificationItem mi[] = new ModificationItem[mods.size()];
				mods.toArray(mi);
				ctx.modifyAttributes(targetName, mi);
			}

			// perform cascading of stuff which has to be done after the new
			// object has been saved.

			for (Iterator<AttributeMapping> i = attributes.iterator(); i.hasNext();) {
				i.next().cascadePostSave(o, tx, ctx);
			}

		} catch (DirectoryException e) {
			throw e;
		} catch (Throwable e) {
			throw new DirectoryException("Can't marshal instance of " + modelClass, e);
		}
	}
	
	
	
	private void renameObjects(Name targetName, DirContext ctx, Object rdn, Object o ,
			Transaction tx, BasicAttributes attrib) throws NamingException, DirectoryException{
		Name newName = targetName.getPrefix(targetName.size() - 1).add(
				rdnAttribute.fieldName + "=" + rdn);
		Name ctxName = ctx.getNameParser("").parse(ctx.getNameInNamespace());

		if (logger.isDebugEnabled()) {
			logger.debug("RDN change: " + targetName + " -> " + newName);
		}

		String dn = getDN(o);
		
		if (this.getModelClass() == Location.class) {
			renameLocality(tx, dn, newName.toString());
		}

		dn = idToUpperCase(dn);
		String tN = idToUpperCase(targetName.toString());
		String nN = idToUpperCase(newName.toString());

		ctx.rename(tN, nN);
		deleteUniqueMember(tx, dn, newName.toString());


		targetName = newName;
		// and tell the object about the new dn
		setDN(ctxName.addAll(newName).toString(), o);

		try {
			// perform cascading of stuff which has to be done after the
			// new object has been saved.
			for (Iterator<AttributeMapping> i = attributes.iterator(); i
					.hasNext();) {
				i.next().cascadeRDNChange(targetName, newName);
			}
		} catch (DirectoryException e) {
			logger.error("Exception during cascade post RDN change", e);
		}

		// let the rdn attribute alone!
		attrib.remove(rdnAttribute.fieldName);
	}
	
	private void updateNormalObjects(Attribute currentAttribute, Attributes currentAttributes ,Attribute a,
			List<ModificationItem> mods, String id) throws NamingException{
		if (currentAttribute != null) {
			if ((a.size() == 1)
					&& a.get(0).equals(ATTRIBUTE_UNCHANGED_MARKER)) {
				currentAttributes.remove(id);
			} else {
				if (!areAttributesEqual(a, currentAttribute)) {
					mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, a));
				}
				currentAttributes.remove(id);
			}
		} else if ((currentAttribute == null) && (a != null)) {
			mods.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, a));
		}
	}
	
	private void updateMember(Attribute currentAttribute, Attributes currentAttributes ,Attribute a,
			List<ModificationItem> mods, String id, Object o, DirContext ctx, Transaction tx) throws NamingException, DirectoryException{
		Class[] memberClasses = ((Group) o).getMemberClasses();

		ArrayList<String> create = new ArrayList<String>();
		for (DirectoryObject obj : getToMakeNew()) {
			for (int i = 0; memberClasses.length > i; i++) {
				if (memberClasses[i] == obj.getClass()) {
					create.add(obj.getDn());
				}
			}
		}

		Attribute attributeToEdit;

		if (a.toString().startsWith("uniqueMember")) {
			attributeToEdit = new BasicAttribute("uniqueMember");
		} else if (a.toString().startsWith("member")) {
			attributeToEdit = new BasicAttribute("member");
		} else if (a.toString().startsWith("memberOf")) {
			attributeToEdit = new BasicAttribute("memberOf");
		} else {
			attributeToEdit = new BasicAttribute("uniquemember");
		}

		if (currentAttribute != null) {
			attributeToEdit = (Attribute) currentAttribute.clone();
		}

		// add new uniqueMembers to attributeToEdit
		if (!create.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("add uniqueMemeber: " + create);
			}
			for (int j = 0; create.size() > j; j++) {
				String memberNew = idToUpperCase(create.get(j));
				if (mutable == true) {
					attributeToEdit.add(memberNew);
				}
			}
			TypeMapping.toMakeNew.clear();
		}

		// remove uniqueMember from attributeToEdit
		if (!getToDelete().isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("delete uniqueMemeber: " + getToDelete());
			}

			for (int j = 0; getToDelete().size() > j; j++) {
				for (int i = 0; attributeToEdit.size() > i; i++) {
					String member1 = idToUpperCase(attributeToEdit.get(i)
							.toString());
					String member2 = idToUpperCase(getToDelete().get(j).getDn());
					if (member1.equalsIgnoreCase(member2)) {
						if (mutable == true) {
							attributeToEdit.remove(i);
						}
					}
				}
			}
			TypeMapping.toDelete.clear();
		}

		// save the changes
		a = (Attribute) attributeToEdit.clone();

		// delete empty uniqueMembers
		for (int i = 0; a.size() > i; i++) {
			if (a.get(i).equals("")) {
				a.remove(i);
			}
		}
		if (a.size() > 0) {
			 if (currentAttribute != null) {
			if (!areAttributesEqual(a, currentAttribute)) {
				mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, a));
			}
			}
		} else {
			ctx = tx.getContext(modelClass);
			if (a.size() == 0) {
				a.add(OneToManyMapping.getDUMMY_MEMBER());
			}
			mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, a));
		}
	}
	
	
	private Attributes getClearedAttributes(BasicAttributes nowAttributes, Attributes ldapAttributes) throws NamingException{
		
		//ignore objectClasses
		nowAttributes.remove("objectClass");
		ldapAttributes.remove("objectClass");
		
		NamingEnumeration<String> nowIDs = nowAttributes.getIDs();
		
		while (nowIDs.hasMore()) {
			String id = nowIDs.next();
			ldapAttributes.remove(id);
			
		}
		return ldapAttributes;
	}

	/**
	 * @param a1
	 * @param a2
	 * @return
	 * @throws NamingException
	 */
	private boolean areAttributesEqual(Attribute a1, Attribute a2)
			throws NamingException {
		if (!a1.getID().equalsIgnoreCase(a2.getID())) {
			return false;
		}

		if ((a1.get() == null) && (a2.get() == null)) {
			return true;
		}

		if ((a1.get() == null) || (a2.get() == null)) {
			return false;
		}

		if (a1.size() != a2.size()) {
			return false;
		}

		for (int i = 0; i < a1.size(); i++) {
			if (a1.get() instanceof byte[]) {
				return Arrays.equals((byte[]) a1.get(), (byte[]) a2.get());
			} else {
				return a1.get(i).equals(a2.get(i));
			}
		}
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
		for (Iterator<AttributeMapping> i = attributes.iterator(); i.hasNext();) {
			i.next().dehydrate(o, a); // there are different dehydrate !!!
		}

		// add object classes
		Attribute objectClassesAttribute = new BasicAttribute("objectClass");
		for (String oc : objectClasses) {
			objectClassesAttribute.add(oc);
		}
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

		if (logger.isDebugEnabled()) {
			logger.debug("Deleting object of " + o.getClass());
		}

		// break cycles
		if (tx.didAlreadyProcessEntity(o)) {
			return true;
		}
		tx.addEntity(o);

		String dn = getDN(o);
		if (null == dn) {
			throw new DirectoryException(
					"Can't delete this object: no DN (mayby it wasn't saved before?)");
		}
		try {

			DirContext ctx = tx.getContext(modelClass);
			// checkMutable(ctx);
			// the dn will frequently be a descendant of the ctx's name. If
			// this
			// is the case, the prefix is removed.
			Name targetName = makeRelativeName(dn, ctx);

			// remove from cache
			tx.purgeCacheEntry(targetName);

			deleteUniqueMember(tx, dn, "");
			deleteRecursively(ctx, targetName, tx);

			try {
				// perform cascading of stuff which has to be done after the
				// new
				// object
				// has been saved.
				for (Iterator<AttributeMapping> i = attributes.iterator(); i.hasNext();) {
					i.next().cascadeDelete(targetName, tx);
				}
			} catch (DirectoryException e) {
				logger.error("Exception during cascade post RDN change", e);
			}
			return true;

		} catch (NameNotFoundException e) {
			logger.warn("Object to be deleted was not actually found.");
			return false;
		} catch (Exception e) {
			throw new DirectoryException("Can't load object", e);
		}
	}

	/**
	 * @param ctx
	 * @param targetName
	 * @param tx
	 * @throws NamingException
	 */
	public static void deleteRecursively(DirContext ctx, Name targetName,
			Transaction tx) throws NamingException {

		NamingEnumeration<NameClassPair> children = ctx.list(targetName);
		try {
			while (children.hasMore()) {
				NameClassPair child = children.next();
				targetName.add(child.getName());
				deleteRecursively(ctx, targetName, tx);
				targetName.remove(targetName.size() - 1);
			}
		} finally {
			children.close();
		}

		if (logger.isDebugEnabled()) {
			logger.debug("destroySubcontext: " + targetName);
		}
		try {
			ctx.destroySubcontext(targetName);
		} catch (Exception e) {
		}

	}

	/**
	 * @author goldml
	 * 
	 * Delete in all other objects the dn of this object as uniqueMember.
	 */
	private void deleteUniqueMember(Transaction tx, String dnMember,
			String newName) throws NamingException, DirectoryException {
		dnMember = idToUpperCase(dnMember);

		Class[] classes = new Class[]{Realm.class,UserGroup.class, ApplicationGroup.class,
				Application.class, Printer.class, Device.class, Location.class};

		Set<DirectoryObject> set = new HashSet<DirectoryObject>();

		for (Class cl : classes) {
			Set<DirectoryObject> list = getMapping().list(cl);
			set.addAll(list);
		}

		for (DirectoryObject o : set) {
			DirContext ctx = tx.getContext(o.getClass());

			if(o.getClass() == Realm.class) {
				Realm realm = (Realm) o;
				o = realm.getAdministrators();
			}
			
			Name targetName = makeRelativeName(getDN(o), ctx);

			Attributes attrs = ctx.getAttributes(targetName);

			Attribute a = attrs.get("uniquemember");

			if (a != null) {
				for (int i = 0; a.size() > i; i++) {
					if (a.get(i).equals(dnMember)) {
						a.remove(i);

						if (a.size() == 0) {
							String dummy = OneToManyMapping.getDUMMY_MEMBER();
							a.add(dummy);
						}

						// for the rename of admins
						// if(!newName.equals("") &&
						// dnGroup.equals("cn=administrators,OU=RealmConfiguration") )
						// {

						if (!newName.equals("")) {
							String mod = idToUpperCase(newName + ","
									+ ctx.getNameInNamespace());
							a.add(mod);
						}

						ModificationItem[] mods = new ModificationItem[1];
						mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, a);

						ctx.modifyAttributes(targetName, mods);
					}
				}
			}
		}
	}

	private void renameLocality(Transaction tx, String dnMember, String newName)
			throws NamingException, DirectoryException {

		DirContext ctx = tx.getContext(Client.class);

		Class[] classes = new Class[]{Client.class};

		Set<DirectoryObject> set = new HashSet<DirectoryObject>();

		for (Class cl : classes) {
			Set<DirectoryObject> list = getMapping().list(cl);
			set.addAll(list);
		}

		for (DirectoryObject o : set) {
			Name targetName = makeRelativeName(getDN(o), ctx);

			Attributes attrs = ctx.getAttributes(targetName);

			Attribute a = attrs.get("l");

			if (a != null) {
				for (int i = 0; a.size() > i; i++) {
					
					if (a.get(i).equals(dnMember)) {

						if (a.size() == 0) {
							String dummy = OneToManyMapping.getDUMMY_MEMBER();
							a.add(dummy);
						}

						if (!newName.equals("")) {
							a.remove(i);
							String mod = "";
							if(newName.startsWith("L")) {
								mod = idToUpperCase(newName) + "," + ctx.getNameInNamespace();
							}
							
							if(newName.startsWith("l")) {
								mod = idToLowerCase(newName) + "," + ctx.getNameInNamespace();
							}
							a.add(mod);
						}

						ModificationItem[] mods = new ModificationItem[1];
						mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, a);

						ctx.modifyAttributes(targetName, mods);
					}
				}
			}
		}
	}

	/**
	 * 
	 */
	protected void initPostLoad() {
		for (AttributeMapping am : attributes) {
			am.initPostLoad();
		}

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
		if (!dnAttribute.getFieldType().equals(String.class)) {
			throw new IllegalArgumentException(
					"The DN Attribute must be of type string");
		}

		this.dnAttribute = dnAttribute;
		dnAttribute.setTypeMapping(this);
	}

	/**
	 * @param o
	 * @return
	 * @throws DirectoryException
	 */
	String getDN(Object o) throws DirectoryException {
		// Logger.getLogger(this.getClass()).debug("get DN for: "+o);
		return (String) dnAttribute.getValue(o);
	}

	public String[] getObjectClasses() {
		return objectClasses;
	}

	/**
	 * @param o
	 * @param tx TODO
	 * @throws DirectoryException
	 */
	public void refresh(Object o, Transaction tx) throws DirectoryException {
		try {
			try {
				DirContext ctx = tx.getContext(modelClass);
				String dn = getDN(o);

				// the dn will frequently be a descendant of the ctx's name. If this
				// is the case, the prefix is removed.
				Name targetName = makeRelativeName(dn, ctx);

				String name = targetName.toString();

				if (logger.isDebugEnabled()) {
					logger.debug("refreshing object of " + modelClass + " for dn=" + dn);
				}

				hydrateInstance(ctx.getAttributes(name), o, tx);

				for (AttributeMapping am : attributes) {
					am.cascadePostLoad(o, tx);
				}

				// update object in cache, no matter what
				Name absoluteName = makeAbsoluteName(name, ctx);
				tx.putCacheEntry(absoluteName, o);

			} catch (NameNotFoundException n) {
				logger.warn("Object doesn't exists anymore !!!");
			}
		} catch (Exception e) {
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

		for (AttributeMapping am : attributes) {
			AttributeMapping clonedAM = am.clone();
			clone.add(clonedAM);
		}

		return clone;
	}

	public void setBaseDN(String baseDN) {
		this.baseDN = baseDN;
	}

	public static boolean getIsNewAction() {
		return isNewAction;
	}

	public static ArrayList<DirectoryObject> getToDelete() {
		return toDelete;
	}

	public static ArrayList<DirectoryObject> getToMakeNew() {
		return toMakeNew;
	}

	public static void setIsNewAction(boolean isNewAction) {
		TypeMapping.isNewAction = isNewAction;
	}

	public static void setToDelete(DirectoryObject td) {
		if (!TypeMapping.toDelete.contains(td)) {
			TypeMapping.toDelete.add(td);
		}
	}

	public static void setToMakeNew(DirectoryObject tmn) {
		if (!TypeMapping.toMakeNew.contains(tmn)) {
			TypeMapping.toMakeNew.add(tmn);
		}
	}

	public static void removeToDelete(DirectoryObject td) {
		if (TypeMapping.toDelete.contains(td)) {
			TypeMapping.toDelete.remove(td);
		}
	}

	public static void removeToMakeNew(DirectoryObject tmn) {
		if (TypeMapping.toMakeNew.contains(tmn)) {
			TypeMapping.toMakeNew.remove(tmn);
		}
	}

	// FIXME: einfacher!
	public static String idToUpperCase(String member) {
		String ret = "";

		member = member.replace("\\,", "#%COMMA%#");

		String[] s = member.split(",");
		for (int i = 0; s.length > i; i++) {
			if (s[i].startsWith("cn="))
				s[i] = s[i].replaceFirst("cn=", "CN=");
			if (s[i].startsWith("dc="))
				s[i] = s[i].replaceFirst("dc=", "DC=");
			if (s[i].startsWith("ou="))
				s[i] = s[i].replaceFirst("ou=", "OU=");
			if (s[i].startsWith("l="))
				s[i] = s[i].replaceFirst("l=", "L=");
			ret = ret + s[i].trim(); // delete whitespaces
			if ((i + 1) < s.length) {
				ret = ret + ",";
			}
		}
		ret = ret.replace("#%COMMA%#", "\\,");
		ret = ret.trim();
		return ret;
	}

	// FIXME: einfacher!
	public static String idToLowerCase(String member) {
		String ret = "";

		member = member.replace("\\,", "#%COMMA%#");

		String[] s = member.split(",");
		for (int i = 0; s.length > i; i++) {
			if (s[i].startsWith("CN="))
				s[i] = s[i].replaceFirst("CN=", "cn=");
			if (s[i].startsWith("DC="))
				s[i] = s[i].replaceFirst("DC=", "dc=");
			if (s[i].startsWith("OU="))
				s[i] = s[i].replaceFirst("OU=", "ou=");
			if (s[i].startsWith("L="))
				s[i] = s[i].replaceFirst("L=", "l=");
			ret = ret + s[i].trim(); // delete whitespaces
			if ((i + 1) < s.length) {
				ret = ret + ",";
			}
		}
		ret = ret.replace("#%COMMA%#", "\\,");
		ret = ret.trim();
		return ret;
	}

	public String getSearchFilter() {
		return searchFilter;
	}

	public void setObjectClasses(String[] objectClasses) {
		this.objectClasses = objectClasses;
	}

	public void setSearchFilter(String searchFilter) {
		this.searchFilter = searchFilter;
	}

	public static DirectoryObject getCurrentObject() {
		return currentObject;
	}

	public static void setCurrentObject(DirectoryObject currentObject) {
		setCurrentObjectName(currentObject.getName());
		TypeMapping.currentObject = currentObject;
	}

	private String getCurrentObjectName() {
		if (TypeMapping.currentObjectName == null)
			return "";
		return TypeMapping.currentObjectName;
	}

	private static void setCurrentObjectName(String currentObjectName) {
		TypeMapping.currentObjectName = currentObjectName;
	}

	public boolean isMutable() {
		return mutable;
	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	public String getKeyClass() {
		return keyClass;
	}
}

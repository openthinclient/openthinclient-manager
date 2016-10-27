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
package org.openthinclient.common.directory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.DirectoryFacade;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.TypeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author levigo
 */
public class LDAPDirectory implements Directory {
	
	private static final Logger logger = LoggerFactory.getLogger(LDAPDirectory.class);

	/**
	 * The RDN used by realms. This is about the only thing which is constant. All
	 * other names may change depending on the type of directory.
	 */
	public static final String REALM_RDN = "ou=RealmConfiguration";

	private final Mapping mapping;

	private final Realm realm;

	private static Set<Class> secondaryClasses = new HashSet<Class>();

	/** A cache of mappings, so we don't need to load them more than once */
	private static final Map<String, Mapping> mappingCache = Collections
			.synchronizedMap(new HashMap<String, Mapping>());

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
	 * @throws DirectoryException
	 */
	private static Mapping loadLDAPMapping(String descriptorName)
			throws IOException, MappingException, ValidationException,
			MarshalException, DirectoryException {
		Mapping m = mappingCache.get(descriptorName);

		if (null == m) {
			// open stream for mapping descriptor
			final InputStream is = LDAPDirectory.class
					.getResourceAsStream(descriptorName + ".xml");
			if (null == is)
				throw new DirectoryException("Can't load mapping of type "
						+ descriptorName);

			m = Mapping.load(is);
			m.initialize();

			mappingCache.put(descriptorName, m);
		}

		return m;
	}

	/**
	 * @throws MappingException
	 * @throws IOException
	 * @throws ValidationException
	 * @throws MarshalException
	 */
	private LDAPDirectory(Mapping mapping, Realm realm) {
		this.mapping = mapping;
		this.realm = realm;
	}

	/**
	 * List all realms found in the directory specified by the environment.
	 * 
	 * @throws DirectoryException
	 */
	public static Set<Realm> listRealms(LDAPConnectionDescriptor lcd)
			throws DirectoryException {
		try {
			final DirectoryFacade df = lcd.createDirectoryFacade();
			final Mapping rootMapping = loadMapping(df);

			try {
				final Set<Realm> realms = rootMapping.list(Realm.class);
				// the realm env is based on the realm dn minus the realm's RDN,
				// i.e.
				// the last part.
				for (final Realm realm : realms) {
					// strip Realm DN of last part
					String realmDN = realm.getDn();
					realmDN = realmDN.replaceFirst("^[^,]+,", "");
					// configure new lcd
					final LDAPConnectionDescriptor d = new LDAPConnectionDescriptor(lcd);
					d.setBaseDN(realmDN);
					realm.setConnectionDescriptor(d);
					rootMapping.refresh(realm);
				}
				return realms;
			} finally {
				rootMapping.close();
			}
		} catch (final DirectoryException e) {
			throw e;
		} catch (final Exception e) {
			throw new DirectoryException("Can't init mapping", e);
		}
	}

	/**
	 * List all realms found in the directory specified by the environment.
	 * 
	 * @throws DirectoryException
	 */
	public static Set<Realm> findAllRealms(LDAPConnectionDescriptor lcd)
			throws DirectoryException {
		try {
			final Set<Realm> realms = new HashSet<Realm>();
			final List<String> partitions = listPartitions(lcd);
			for (final String partition : partitions) {
				final LDAPConnectionDescriptor d = new LDAPConnectionDescriptor(lcd);
				d.setBaseDN(partition);
				try {
					realms.addAll(listRealms(d));
				} catch (final DirectoryException e) {
					logger.error("Can't list realms for partition " + partition
							+ ". Skipping it.", e);
				}
			}

			return realms;
		} catch (final Exception e) {
			throw new DirectoryException("Can't init mapping", e);
		}
	}

	/**
	 * @param env
	 * @return
	 * @throws NamingException
	 */
	public static List<String> listPartitions(LDAPConnectionDescriptor lcd)
			throws NamingException {
		final List<String> partitions = new ArrayList<String>();
		try {
			final DirContext ctx = lcd.createDirectoryFacade().createDirContext();
			try {
				final Attributes a = ctx.getAttributes("",
						new String[]{"namingContexts"});
				final Attribute namingContexts = a.get("namingContexts");
				if (null == namingContexts)
					throw new NamingException(
							"Directory doesn't supply a list of partitions.");
				final NamingEnumeration<?> allAttributes = namingContexts.getAll();
				while (allAttributes.hasMore())
					partitions.add(allAttributes.next().toString());
			} finally {
				if (null != ctx)
					ctx.close();
			}
		} catch (final NamingException e) {
			throw e;
		} catch (final Exception e) {
			throw new NamingException("Can't open connection: " + e);
		}
		return partitions;
	}

	/**
	 * @param lcd
	 * @return
	 * @throws IOException
	 * @throws MappingException
	 * @throws ValidationException
	 * @throws MarshalException
	 * @throws DirectoryException
	 * @throws MappingException
	 * @throws IOException
	 * @throws MarshalException
	 * @throws ValidationException
	 * @throws NamingException
	 * @throws UnsupportedCallbackException
	 */
	private static Mapping loadMapping(DirectoryFacade df)
			throws DirectoryException, ValidationException, MarshalException,
			IOException, MappingException, NamingException {

		// create copy of loaded mapping to prevent "tainting" the cache.
		final Mapping mapping = new Mapping(loadLDAPMapping(df.guessDirectoryType()
				.toString()));
		mapping.setDirectoryFacade(df);
		return mapping;

	}

	/**
	 * Create an LDAPDirectory for the realm found at the directory location
	 * specified by the given enviromnent. The provider url supplied in the
	 * environment must point to a openthinclient.org base dn, i.e. the element
	 * containing the ou=RealmConfiguration.
	 * 
	 * @throws DirectoryException
	 */
	public static LDAPDirectory openRealm(Realm realm) throws DirectoryException {
		try {
			final LDAPConnectionDescriptor lcd = realm.getConnectionDescriptor();
			assertBaseDNReachable(lcd);

			final DirectoryFacade df = lcd.createDirectoryFacade();
			final Mapping rootMapping = loadMapping(df);
			rootMapping.setDirectoryFacade(df);
			rootMapping.refresh(realm);

			String version = realm.getValue("UserGroupSettings.DirectoryVersion");
			final String secondaryUrlString = realm
					.getValue("Directory.Secondary.LDAPURLs");

			if (null == version)
				version = "";

			secondaryClasses = new HashSet<Class>();
			if (version.equals("secondary") && null != secondaryUrlString)
				if (null != secondaryUrlString) {
					final LDAPConnectionDescriptor secLcd = realm
							.createSecondaryConnectionDescriptor();
					try {
						assertBaseDNReachable(secLcd);

						final DirectoryFacade secondaryDF = secLcd.createDirectoryFacade();
						final Mapping secondaryMapping = loadMapping(secondaryDF);

						if (realm.getValue("UserGroupSettings.Type").equals("Users")) {
							copyTypeMapping(rootMapping, secondaryMapping, User.class);
							secondaryClasses.add(User.class);
						}

						if (realm.getValue("UserGroupSettings.Type").equals("UsersGroups")) {
							copyTypeMapping(rootMapping, secondaryMapping, User.class,
									UserGroup.class);

							secondaryClasses.add(User.class);
							secondaryClasses.add(UserGroup.class);
						}

						if (realm.getValue("UserGroupSettings.Type").equals(
								"NewUsersGroups"))
							copyTypeMapping(rootMapping, secondaryMapping, User.class,
									UserGroup.class);
					} catch (final Exception e) {
						logger.error(e.getMessage(), e);
					} finally {
						rootMapping.refresh(realm);
					}
				}

			try {
				return new LDAPDirectory(rootMapping, realm);
			} finally {
				rootMapping.close();
			}
		} catch (final DirectoryException e) {
			throw e;
		} catch (final Exception e) {
			throw new DirectoryException("Can't init directory", e);
		}
	}

	private static void copyTypeMapping(final Mapping rootMapping,
			final Mapping secondaryMapping, Class... type) {
		final List<TypeMapping> relocated = new LinkedList<TypeMapping>();
		for (final Class c : type) {
			final TypeMapping typeMapping = secondaryMapping.getTypes().get(c);
			rootMapping.add(typeMapping);
			relocated.add(typeMapping);
		}

		for (final TypeMapping typeMapping : relocated)
			typeMapping.initPostLoad();
	}

	public static void assertBaseDNReachable(LDAPConnectionDescriptor lcd)
			throws NamingException, DirectoryException {
		final LdapContext ctx = lcd.createDirectoryFacade().createDirContext();
		try {
			ctx.getAttributes("");
		} catch (final NameNotFoundException e) {
			throw new DirectoryException("The realm at " + lcd.getLDAPUrl()
					+ " cannot be reached");
		} finally {
			ctx.close();
		}
	}

	/**
	 * Create an LDAPDirectory for the specified environment. Don't try to open a
	 * realm etc.
	 * 
	 * @throws DirectoryException
	 */
	public static LDAPDirectory openEnv(LDAPConnectionDescriptor lcd)
			throws DirectoryException {
		try {
			return new LDAPDirectory(loadMapping(lcd.createDirectoryFacade()), null);
		} catch (final DirectoryException e) {
			throw e;
		} catch (final Exception e) {
			throw new DirectoryException("Can't init directory", e);
		}
	}

	public <T> T create(Class<T> type) throws DirectoryException {
		assertInitialized();
		return mapping.create(type);
	}

	public boolean delete(Object object) throws DirectoryException {
		assertInitialized();
		return mapping.delete(object);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openthinclient.common.directory.Directory#list(java.lang.Class)
	 */
	public <T> Set<T> list(Class<T> type) throws DirectoryException {
		assertInitialized();
		final Set<T> list = mapping.list(type);
		associateWithRealm(list);

		return list;
	}

	/**
	 * @param <T>
	 * @param list
	 */
	private <T> void associateWithRealm(Set<T> list) {
		for (final T t : list)
			if (t instanceof DirectoryObject)
				((DirectoryObject) t).setRealm(realm);
	}

	/**
	 * List objects of the specified class, using the given search filter and
	 * search scope.
	 * 
	 * @param type type of object to list
	 * @param filter the search filter. All objects will be returned, if the
	 *          filter is <code>null</code>.
	 * @param scope the search scope.
	 * @return
	 * @throws DirectoryException
	 */
	public <T> Set<T> list(Class<T> type, Filter filter,
			TypeMapping.SearchScope scope) throws DirectoryException {
		assertInitialized();

		final Set<T> list = mapping.list(type, filter, null, scope);
		associateWithRealm(list);

		return list;
	}

	/**
	 * Load an object of the given type from the specified dn
	 * 
	 * @param type the type of object to load
	 * @param dn the absolute dn of the object to load
	 * @return the loaded instance
	 * @throws DirectoryException
	 */
	public <T> T load(Class<T> type, String dn) throws DirectoryException {
		assertInitialized();

		final T load = mapping.load(type, dn, false);

		return load;
	}

	/**
	 * Load an object of the given type from the specified dn, optionally skipping
	 * objects found in the cache
	 * 
	 * @param type the type of object to load
	 * @param dn the absolute dn of the object to load
	 * @param noCache whether to ignore cached objects
	 * @return loaded instance
	 * @throws DirectoryException
	 */
	public <T> T load(Class<T> type, String dn, boolean noCache)
			throws DirectoryException {
		assertInitialized();

		final T load = mapping.load(type, dn, noCache);
		if (load instanceof DirectoryObject)
			((DirectoryObject) load).setRealm(realm);

		return load;
	}

	/**
	 * Save the given object into the directory. The object will be saved at the
	 * default location determined by the object type.
	 * 
	 * @param object the object to save
	 * @throws DirectoryException
	 */
	public void save(Object object) throws DirectoryException {
		assertInitialized();
		mapping.save(object, null);
		mapping.refresh(object);
	}

	/**
	 * Save the given object into the directory, optionally at a specified
	 * location.
	 * 
	 * @param object the object to save
	 * @param baseDN the absolute dn of the object below which to save the object.
	 *          If set to <code>null</code>, the default location will be
	 *          chosen based on the object type.
	 * @throws DirectoryException
	 */
	public void save(Object object, String baseDN) throws DirectoryException {
		assertInitialized();
		mapping.save(object, baseDN);
		mapping.refresh(object);
	}

	private void assertInitialized() {
		if (null == mapping)
			throw new IllegalStateException("Not initialized");
	}

	public void refresh(Object o) throws DirectoryException {
		assertInitialized();

		mapping.refresh(o);

		if (o instanceof DirectoryObject)
			((DirectoryObject) o).setRealm(realm);
	}

	/**
	 * @return
	 * @deprecated Try to do without access to the underlying mapping.
	 */
	@Deprecated
	public Mapping getMapping() {
		return mapping;
	}

	@Deprecated
	public static String idToUpperCase(String member) {
		String ret = "";

		member = member.replace("\\,", "#%COMMA%#");

		final String[] s = member.split(",");
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
			if (i + 1 < s.length)
				ret = ret + ",";
		}
		ret = ret.replace("#%COMMA%#", "\\,");
		ret = ret.trim();
		return ret;
	}

	// FIXME: change to: ask Realm or DirectoryFacade
	@Deprecated
	public static boolean isMutable(Class currentClass) {
		for (final Class secClass : secondaryClasses)
			if (currentClass == secClass)
				return false;
		return true;
	}

}

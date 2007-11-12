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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.log4j.Logger;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.util.UsernamePasswordHandler;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.TypeMapping;

/**
 * @author levigo
 */
public class LDAPDirectory implements Directory {
	private static final Logger logger = Logger.getLogger(LDAPDirectory.class);

	/**
	 * The RDN used by realms. This is about the only thing which is constant. All
	 * other names may change depending on the type of directory.
	 */
	public static final String REALM_RDN = "ou=RealmConfiguration";

	private final Mapping mapping;

	private final Realm realm;

	private final Hashtable<String, String> allSettings = new Hashtable<String, String>();

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
			InputStream is = LDAPDirectory.class.getResourceAsStream(descriptorName
					+ ".xml");
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
			Mapping rootMapping = loadMapping(lcd);
			try {

				Set<Realm> realms = rootMapping.list(Realm.class);
				// the realm env is based on the realm dn minus the realm's RDN,
				// i.e.
				// the last part.
				for (Realm realm : realms) {
					// strip Realm DN of last part
					String realmDN = realm.getDn();
					realmDN = realmDN.replaceFirst("^[^,]+,", "");
					// configure new lcd
					LDAPConnectionDescriptor d = new LDAPConnectionDescriptor(lcd);
					d.setBaseDN(realmDN);
					realm.setConnectionDescriptor(d);
				}
				return realms;
			} finally {
				rootMapping.close();
			}
		} catch (DirectoryException e) {
			throw e;
		} catch (Exception e) {
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
			Set<Realm> realms = new HashSet<Realm>();
			List<String> partitions = listPartitions(lcd);
			for (String partition : partitions) {
				LDAPConnectionDescriptor d = new LDAPConnectionDescriptor(lcd);
				d.setBaseDN(partition);
				try {
					realms.addAll(listRealms(d));
				} catch (DirectoryException e) {
					logger.error("Can't list realms for partition " + partition
							+ ". Skipping it.", e);
				}
			}

			return realms;
		} catch (Exception e) {
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
		List<String> partitions = new ArrayList<String>();
		try {
			DirContext ctx = lcd.createInitialContext();
			try {
				Attributes a = ctx.getAttributes("", new String[]{"namingContexts"});
				Attribute namingContexts = a.get("namingContexts");
				if (null == namingContexts) {
					throw new NamingException(
							"Directory doesn't supply a list of partitions.");
				}
				NamingEnumeration<?> allAttributes = namingContexts.getAll();
				while (allAttributes.hasMore()) {
					partitions.add(allAttributes.next().toString());
				}
			} finally {
				if (null != ctx) {
					ctx.close();
				}
			}
		} catch (NamingException e) {
			throw e;
		} catch (Exception e) {
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
	private static <SelectBasePanel, InitEnvironmentPanel, ConnectionSettingsWizardPanel, createLDAPConnectionDescriptor, ConnectionSettingsVisualPanel> Mapping loadMapping(
			LDAPConnectionDescriptor lcd) throws DirectoryException,
			ValidationException, MarshalException, IOException, MappingException,
			NamingException {

		Mapping mapping = new Mapping(loadLDAPMapping(lcd.guessServerType()
				.toString()));
		mapping.setDefaultContextEnvironment(lcd.getLDAPEnv());
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
			Mapping rootMapping = loadMapping(realm.getConnectionDescriptor());

			for (TypeMapping tm : rootMapping.getTypes().values()) {
				rootMapping.setEnvPropsForType(tm.getModelClass(), rootMapping
						.getDefaultContextEnvironment());
			}

			// FIXME
			// try {
			// LDAPConnectionDescriptor secondLcd = createNewConnection();
			//
			// Map<Class, TypeMapping> map = new HashMap<Class, TypeMapping>();
			//
			// NameParser np = secondLcd.createInitialContext().getNameParser("");
			// Name secondaryBaseDN = np.parse(secondLcd.getBaseDN());
			//
			// Mapping secondaryMapping = loadMapping(secondLcd);
			//
			// for (Class secClass : secondaryClasses) {
			// rootMapping.setEnvPropsForType(secClass, secondaryMapping
			// .getDefaultContextEnvironment());
			//
			// if (secClass == User.class) {
			// TypeMapping tm = secondaryMapping.getMapping(User.class);
			//
			// if (allSettings.get("types").equals("Users")
			// || allSettings.get("types").equals("UsersGroups")) {
			// tm.setMutable(false);
			// tm.setBaseDN("");
			// }
			// rootMapping.putMapping(tm);
			// rootMapping.setEnvPropsForType(User.class, secondaryMapping
			// .getDefaultContextEnvironment());
			// map.put(User.class, rootMapping.getMapping(User.class));
			// }
			//
			// if (secClass == UserGroup.class) {
			// TypeMapping tm = secondaryMapping.getMapping(UserGroup.class);
			//
			// if (allSettings.get("types").equals("UsersGroups")) {
			// rootMapping.putMapping(secondaryMapping
			// .getMapping(UserGroup.class));
			// tm.setMutable(false);
			// tm.setBaseDN("");
			// }
			// rootMapping.putMapping(tm);
			// rootMapping.setEnvPropsForType(UserGroup.class, secondaryMapping
			// .getDefaultContextEnvironment());
			// map.put(UserGroup.class, rootMapping.getMapping(UserGroup.class));
			// }
			// }
			//
			// rootMapping.setMappersByDirectory(secondaryBaseDN, map);
			//
			// secondaryMapping.close();
			// } catch (Exception e) {
			// rootMapping = loadMapping(realm.getConnectionDescriptor());
			// logger.error("Cannot connect to secondary directory!", e);
			// }
			//
			// Set<Class> classes = rootMapping.getTypes().keySet();
			//
			// NameParser np = realm.getConnectionDescriptor().createInitialContext()
			// .getNameParser("");
			// Name primaryBaseDN = np
			// .parse(realm.getConnectionDescriptor().getBaseDN());
			//
			// Map<Class, TypeMapping> map = new HashMap<Class, TypeMapping>();
			//
			// for (Class c : classes) {
			// TypeMapping tm = rootMapping.getMapping(c);
			// map.put(c, tm);
			// }
			//
			// rootMapping.setMappersByDirectory(primaryBaseDN, map);

			try {
				return new LDAPDirectory(rootMapping, realm);
			} finally {
				rootMapping.close();
			}
		} catch (DirectoryException e) {
			throw e;
		} catch (Exception e) {
			throw new DirectoryException("Can't init directory", e);
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
			return new LDAPDirectory(loadMapping(lcd), null);
		} catch (DirectoryException e) {
			throw e;
		} catch (Exception e) {
			throw new DirectoryException("Can't init directory", e);
		}
	}

	public <T> T create(Class<T> type) throws DirectoryException {
		if (null == mapping) {
			throw new IllegalStateException("Not initialized");
		}
		return mapping.create(type);
	}

	public boolean delete(Object object) throws DirectoryException {
		if (null == mapping) {
			throw new IllegalStateException("Not initialized");
		}
		return mapping.delete(object);
	}

	public <T> Set<T> list(Class<T> type) throws DirectoryException {
		if (null == mapping) {
			throw new IllegalStateException("Not initialized");
		}
		Set<T> list = mapping.list(type);
		associateWithRealm(list);

		return list;
	}

	/**
	 * @param <T>
	 * @param list
	 */
	private <T> void associateWithRealm(Set<T> list) {
		for (T t : list) {
			if (t instanceof DirectoryObject) {
				((DirectoryObject) t).setRealm(realm);
			}
		}
	}

	public <T> Set<T> list(Class<T> type, String baseDN)
			throws DirectoryException {
		if (null == mapping) {
			throw new IllegalStateException("Not initialized");
		}

		Set<T> list = mapping.list(type, baseDN);
		associateWithRealm(list);

		return list;
	}

	public <T> Set<T> list(Class<T> type, String baseDN, Filter filter,
			TypeMapping.SearchScope scope) throws DirectoryException {
		if (null == mapping) {
			throw new IllegalStateException("Not initialized");
		}

		Set<T> list = mapping.list(type, null, filter, baseDN, scope);
		associateWithRealm(list);

		return list;
	}

	public <T> T load(Class<T> type, String dn) throws DirectoryException {
		if (null == mapping) {
			throw new IllegalStateException("Not initialized");
		}

		T load = mapping.load(type, null, dn, false);
		if (load instanceof DirectoryObject) {
			((DirectoryObject) load).setRealm(realm);
		}

		return load;
	}

	public <T> T load(Class<T> type, String dn, boolean noCache)
			throws DirectoryException {
		if (null == mapping) {
			throw new IllegalStateException("Not initialized");
		}

		T load = mapping.load(type, null, dn, noCache);
		if (load instanceof DirectoryObject) {
			((DirectoryObject) load).setRealm(realm);
		}

		return load;
	}

	public void save(Object object) throws DirectoryException {

		if (null == mapping) {
			throw new IllegalStateException("Not initialized");
		}
		mapping.save(object, null, null);
	}

	public void save(Object object, String baseDN) throws DirectoryException {
		if (null == mapping) {
			throw new IllegalStateException("Not initialized");
		}
		mapping.save(object, null, baseDN);
	}

	public void refresh(Object o) throws DirectoryException {
		if (null == mapping) {
			throw new IllegalStateException("Not initialized");
		}

		mapping.refresh(o);

		if (o instanceof DirectoryObject) {
			((DirectoryObject) o).setRealm(realm);
		}
	}

	public Mapping getMapping() {
		return mapping;
	}

	/**
	 * @author goldml
	 * 
	 * The method will create a new connection to a secondary server.
	 */
	public LDAPConnectionDescriptor createNewConnection()
			throws DirectoryException {

		LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();

		lcd.setHostname(allSettings.get("hostname"));
		lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
		lcd
				.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
		lcd.setBaseDN(allSettings.get("baseDN"));

		lcd.setPortNumber((short) Integer.parseInt(allSettings.get("portnumber")));
		lcd.setCallbackHandler(new UsernamePasswordHandler(allSettings
				.get("username"), allSettings.get("password")));

		// for read only
		Hashtable env = new Hashtable();
		env.put(Mapping.MAPPING_IS_MUTABLE, "false");
		lcd.setExtraEnv(env);
		return lcd;
	}

	public static boolean isMutable(Class currentClass) {
		for (Class secClass : secondaryClasses) {
			if (currentClass == secClass)
				return false;
		}
		return true;
	}
}

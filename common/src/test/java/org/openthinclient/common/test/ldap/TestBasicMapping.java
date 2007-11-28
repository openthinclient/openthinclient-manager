package org.openthinclient.common.test.ldap;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Group;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.TypeMapping;
import org.openthinclient.ldap.Util;

@Ignore
// FIXME once it runs
public class TestBasicMapping extends AbstractEmbeddedDirectoryTest {
	private static Class[] groupClasses = {Location.class, UserGroup.class,
			Application.class, ApplicationGroup.class, Printer.class, Device.class,
			HardwareType.class};

	private static Class[] objectClasses = {Location.class, UserGroup.class,
			Application.class, ApplicationGroup.class, Printer.class, Device.class,
			HardwareType.class, User.class, Client.class};

	@Test
	public void createEnvironment() throws IOException, DirectoryException,
			NamingException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();

		createOU("NeueUmgebung", LDAPDirectory.openEnv(lcd));

		lcd.setBaseDN("ou=" + "NeueUmgebung" + "," + lcd.getBaseDN());

		final LdapContext ctx = lcd.createDirContext();

		LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Realm realm = initRealm(dir, "");
		realm.setConnectionDescriptor(lcd);

		// Serversettings
		realm.setValue("Serversettings.Hostname", realm.getConnectionDescriptor()
				.getHostname());

		final Short s = new Short(realm.getConnectionDescriptor().getPortNumber());

		realm.setValue("Serversettings.Portnumber", s.toString());
		final String schemaProviderName = realm.getConnectionDescriptor()
				.getHostname();

		realm.setValue("Serversettings.SchemaProviderName", schemaProviderName);

		dir = LDAPDirectory.openRealm(realm);
		initOUs(ctx, dir);

		initAdmin(dir, realm,
				"administrator", "cn=administrator,ou=users,dc=test,dc=test"); //$NON-NLS-1$

		// final HTTPLdifImportAction action = new HTTPLdifImportAction(lcd
		// .getHostname());
		// action.importOneFromURL("locations", realm);
		//
		//	
		// action.importOneFromURL("hwtypes", realm);
		// action.importOneFromURL("devices", realm);
		System.out.println();

	}

	private void createOU(String newFolderName, LDAPDirectory directory)
			throws DirectoryException {
		final OrganizationalUnit ou = new OrganizationalUnit();
		ou.setName(newFolderName);
		ou.setDescription("openthinclient.org Console"); //$NON-NLS-1$
		directory.save(ou, "");
	}

	private static void initAdmin(LDAPDirectory dir, Realm realm, String name,
			String baseDN) throws DirectoryException {

		baseDN = "cn=" + name + "," + baseDN;
		final User admin = new User();
		admin.setName(name);
		admin.setDescription("Initialer administrativer Benutzer"); //$NON-NLS-1$
		admin.setNewPassword("openthinclient"); //$NON-NLS-1$
		admin.setSn("Admin");
		admin.setGivenName("Joe");
		// admin.setDn(baseDN);
		// admin.setAdmin(true);

		final UserGroup administrators = realm.getAdministrators();
		// administrators.setAdminGroup(true);

		dir.save(admin);
		administrators.getMembers().add(admin);
		realm.setAdministrators(administrators);

		dir.save(administrators);
		dir.save(realm);
	}

	private static void initOUs(DirContext ctx, LDAPDirectory dir)
			throws DirectoryException {
		try {
			final Mapping rootMapping = dir.getMapping();

			final Collection<TypeMapping> typeMappers = rootMapping.getTypes()
					.values();
			for (final TypeMapping mapping : typeMappers) {
				final OrganizationalUnit ou = new OrganizationalUnit();
				final String baseDN = mapping.getBaseRDN();

				// we create only those OUs for which we have a base DN
				if (null != baseDN) {
					ou.setName(baseDN.substring(baseDN.indexOf("=") + 1)); //$NON-NLS-1$

					dir.save(ou, ""); //$NON-NLS-1$
				}
			}

		} catch (final Exception e) {
			throw new DirectoryException("Kann OU-Struktur nicht initialisieren", e); //$NON-NLS-1$
		}
	}

	private static Realm initRealm(LDAPDirectory dir, String description)
			throws DirectoryException {
		try {
			final Realm realm = new Realm();
			realm.setDescription(description);
			final UserGroup admins = new UserGroup();
			admins.setName("administrators"); //$NON-NLS-1$
			// admins.setAdminGroup(true);
			realm.setAdministrators(admins);

			final String date = new Date().toString();
			realm.setValue("invisibleObjects.initialized", date); //$NON-NLS-1$

			final User roPrincipal = new User();
			roPrincipal.setName("roPrincipal");
			roPrincipal.setSn("Read Only User");
			roPrincipal.setNewPassword("secret");
			// roPrincipal.setAdmin(true);

			realm.setReadOnlyPrincipal(roPrincipal);
			// realm.getProperties().setDescription("realm"); // ???

			realm.setDescription("realm");

			dir.save(realm, "");

			return realm;
		} catch (final Exception e) {
			throw new DirectoryException("Kann Umgebung nicht erstellen", e); //$NON-NLS-1$
		}
	}

	@Test
	public void createNewObjects() throws DirectoryException {

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();

		final Location location = new Location();

		final HardwareType hwtype = new HardwareType();

		final Device device = new Device();

		final User user = new User();

		final UserGroup group = new UserGroup();

		final Client client = new Client();

		final Application application = new Application();

		final ApplicationGroup appGroup = new ApplicationGroup();

		final Printer printer = new Printer();

		final Set<DirectoryObject> objects = new HashSet<DirectoryObject>();

		location.setName("l1");
		location.setDn("cn=l1,ou=locations");
		objects.add(location);

		hwtype.setName("h1");
		hwtype.setDn("cn=h1,ou=hwtypes");
		objects.add(hwtype);

		device.setName("d1");
		device.setDn("cn=d1,ou=devices");
		objects.add(device);

		user.setName("u1");
		user.setDn("cn=u1,ou=users");
		objects.add(user);

		group.setName("g1");
		group.setDn("cn=g1,ou=usergroups");
		objects.add(group);

		printer.setName("p1");
		printer.setDn("cn=p1,ou=printers");
		objects.add(printer);

		client.setName("tc1");
		final Set<Device> devices = new HashSet<Device>();
		devices.add(device);
		client.setHardwareType(hwtype);
		client.setLocation(location);
		client.setDn("cn=tc1,ou=clients");
		objects.add(client);

		application.setName("a1");
		application.setDn("cn=a1,ou=apps");
		objects.add(application);

		appGroup.setName("ag1");
		appGroup.setDn("cn=ag1,ou=appgroups");
		objects.add(appGroup);

		lcd.setBaseDN("ou=NeueUmgebung,dc=test,dc=test");

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		for (final DirectoryObject obj : objects)
			dir.save(obj);

		final Set<DirectoryObject> currentDirObjetSet = new HashSet<DirectoryObject>();

		for (final DirectoryObject o : objects) {
			dir.refresh(o);
			currentDirObjetSet.add(o);
		}

		Assert.assertNotSame(objects, currentDirObjetSet);
	}

	@Test
	public void assignObjects() throws DirectoryException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN("ou=NeueUmgebung,dc=test,dc=test");

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Set<User> users = dir.list(User.class);

		final Set<Location> locations = dir.list(Location.class);

		final Set<UserGroup> userGroups = dir.list(UserGroup.class);

		final Set<Client> clients = dir.list(Client.class);

		final Set<Application> applications = dir.list(Application.class);

		final Set<ApplicationGroup> applicationGroups = dir
				.list(ApplicationGroup.class);

		final Set<Printer> printers = dir.list(Printer.class);

		final Set<DirectoryObject> groupSet = new HashSet<DirectoryObject>();

		final Set<DirectoryObject> allMembers = new HashSet<DirectoryObject>();

		for (final UserGroup group : userGroups) {
			group.setMembers(users);
			allMembers.addAll(users);

			groupSet.add(group);
		}

		for (final Application group : applications) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();

			members.addAll(users);
			members.addAll(userGroups);
			members.addAll(clients);
			members.addAll(applicationGroups);

			group.setMembers(members);
			allMembers.addAll(members);
			groupSet.add(group);
		}

		for (final ApplicationGroup group : applicationGroups) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();

			members.addAll(users);
			members.addAll(userGroups);
			members.addAll(clients);

			group.setMembers(members);
			allMembers.addAll(members);
			groupSet.add(group);
		}

		for (final Printer group : printers) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();

			members.addAll(users);
			members.addAll(userGroups);
			members.addAll(clients);
			members.addAll(locations);

			group.setMembers(members);
			allMembers.addAll(members);
			groupSet.add(group);
		}

		for (final DirectoryObject o : groupSet)
			dir.save(o);

		final Set<DirectoryObject> currentMembers = new HashSet<DirectoryObject>();

		for (final DirectoryObject o : groupSet)
			if (o instanceof Group) {
				dir.refresh(o);
				final Group g = (Group) o;
				currentMembers.addAll(g.getMembers());
			}

		Assert.assertNotSame(allMembers, currentMembers);
	}

	@Test
	public void removeAssignements() throws DirectoryException {

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN("ou=NeueUmgebung,dc=test,dc=test");

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Set<Group> groupSet = new HashSet<Group>();

		for (final Class cl : groupClasses)
			for (final Object o : dir.list(cl))
				if (o instanceof Group)
					groupSet.add((Group) o);

		final Set<DirectoryObject> currentMembers = new HashSet<DirectoryObject>();

		for (final Group group : groupSet)
			group.setMembers(null);

		for (final Group o : groupSet) {
			dir.refresh(o);
			currentMembers.addAll(o.getMembers());
		}

		Assert.assertTrue("All Assignements deltet", 0 == currentMembers.size());

	}

	@Test
	public void assignObjectsAgain() throws DirectoryException {
		// assignObjects();
	}

	@Test
	public void deleteObjects() throws DirectoryException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN("ou=NeueUmgebung,dc=test,dc=test");

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Set<DirectoryObject> objects = new HashSet<DirectoryObject>();

		for (final Class cl : objectClasses)
			for (final Object o : dir.list(cl))
				if (o instanceof DirectoryObject)
					objects.add((DirectoryObject) o);

		if (objects.size() > 0)
			for (final DirectoryObject obj : objects)
				dir.delete(obj);

		final Set<DirectoryObject> currentDirObjectSet = new HashSet<DirectoryObject>();

		for (final DirectoryObject o : objects) {
			dir.refresh(o);

			currentDirObjectSet.add(o);
		}

		Assert.assertNotSame(objects, currentDirObjectSet);

	}

	@Test
	public void deleteEnvironment() throws NamingException, DirectoryException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN("ou=NeueUmgebung,dc=test,dc=test");

		final Name targetName = Util.makeRelativeName("", lcd);

		final LdapContext ctx = lcd.createDirContext();
		try {
			Util.deleteRecursively(ctx, targetName);
		} finally {

			// final Set<Realm> realms = LDAPDirectory.listRealms(lcd);
			//
			// Assert.assertNull(realms);

			ctx.close();
		}

	}

	// @Test
	// public void testSomething() {
	// System.setProperty("foo", "bar");
	//
	// Assert.assertEquals("System property correct", "bar", System
	// .getProperty("foo"));
	// }
	//
	// @Test
	// public void testSomethingElse() {
	// Assert.assertNotNull("System property: os.name", System
	// .getProperty("os.name"));
	// }
	//
	// @Test
	// public void testYetSomething() {
	// try {
	// new FileInputStream("c:/doesntexist");
	// Assert.fail("Expected exception not thrown");
	// } catch (FileNotFoundException e) {
	// // expected
	// }
	// }

}

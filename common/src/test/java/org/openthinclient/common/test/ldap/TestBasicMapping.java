package org.openthinclient.common.test.ldap;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.sar.DirectoryService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openthinclient.common.directory.ACLUtils;
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
import org.openthinclient.console.HTTPLdifImportAction;
import org.openthinclient.console.NewRealmInitCommand;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.Util;

public class TestBasicMapping {
	private static DirectoryService ds;

	@BeforeClass
	public static void setUp() throws Exception {
		ds = new DirectoryService();
		ds.setEmbeddedAccessControlEnabled(false);
		ds.setEmbeddedAnonymousAccess(true);
		ds.setEmbeddedServerEnabled(true);
		ds
				.setContextFactory("org.apache.directory.server.jndi.ServerContextFactory");
		ds.setContextProviderURL("uid=admin,ou=system");
		ds.setContextSecurityAuthentication("simple");
		ds.setContextSecurityCredentials("secret");
		ds.setContextSecurityPrincipal("uid=admin,ou=system");

		ds.setEmbeddedCustomRootPartitionName("dc=test,dc=test");
		ds.setEmbeddedWkdir("unit-test-tmp");

		// ds.setEmbeddedLDIFdir("${jboss.server.data.dir}/apacheds-ldif");
		// <attribute name="EmbeddedCustomBootstrapSchema">
		// <xml-properties>
		// <config-property
		//
		// name="NisSchema">org.apache.directory.server.core.schema.bootstrap.NisSchema</config-property>
		// </xml-properties>
		// </attribute>

		ds.setEmbeddedEnableNtp(false);
		ds.setEmbeddedEnableKerberos(false);
		ds.setEmbeddedEnableChangePassword(false);
		ds.setEmbeddedLDAPNetworkingSupport(true);
		ds.setEmbeddedLDAPPort(11389);
		ds.setEmbeddedLDAPSPort(11636);

		ds.start();
	}

	@AfterClass
	public static void cleanUp() {
		if (null != ds)
			ds.stop();

		deleteRecursively(new File("unit-test-tmp"));
	}

	private static void deleteRecursively(File file) {
		if (!file.exists())
			return;

		if (file.isDirectory())
			for (final File f : file.listFiles())
				if (f.isDirectory())
					deleteRecursively(f);
				else
					f.delete();

		file.delete();
	}

	private static Class[] groupClasses = {Location.class, UserGroup.class,
			Application.class, ApplicationGroup.class, Printer.class, Device.class,
			HardwareType.class};

	private static Class[] objectClasses = {Location.class, UserGroup.class,
			Application.class, ApplicationGroup.class, Printer.class, Device.class,
			HardwareType.class, User.class, Client.class};

	@Test
	public void createNewEnvironment() throws DirectoryException,
			NamingException, IOException {

		final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();

		lcd.setBaseDN("");

		final NewRealmInitCommand command = new NewRealmInitCommand();

		LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		command.createOU("ou=NeueUmgebung", dir);

		lcd.setBaseDN("ou=NeueUmgebung");

		final Realm realm = NewRealmInitCommand.initRealm(dir, "");
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

		final LdapContext ctx = lcd.createDirContext();

		// init OUs
		NewRealmInitCommand.initOUs(ctx, dir);

		// init Admin
		NewRealmInitCommand.initAdmin(dir, realm, "administrator", null); //$NON-NLS-1$

		// import LDIF
		final HTTPLdifImportAction action = new HTTPLdifImportAction(lcd
				.getHostname());
		action.importOneFromURL("locations", realm);

		action.importOneFromURL("hwtypes", realm);
		action.importOneFromURL("devices", realm);

		final ACLUtils aclUtils = new ACLUtils(ctx);
		aclUtils.makeACSA(""); //$NON-NLS-1$

		aclUtils.enableSearchForAllUsers(""); //$NON-NLS-1$

		aclUtils.enableAdminUsers(""); //$NON-NLS-1$

		// Asserts
		final Realm currentRealm = new Realm(lcd);
		Assert.assertEquals(realm, currentRealm);

		final UserGroup currentAdmins = currentRealm.getAdministrators();
		Assert.assertEquals(currentAdmins, realm.getAdministrators());

		final Set<Location> locations = dir.list(Location.class);
		final Set<Device> devices = dir.list(Device.class);
		final Set<HardwareType> hwtypes = dir.list(HardwareType.class);

		Assert.assertNotNull(locations);
		Assert.assertNotNull(devices);
		Assert.assertNotNull(hwtypes);

		final Set<OrganizationalUnit> ous = dir.list(OrganizationalUnit.class);

		boolean ouSet = false;

		if (ous.size() > 1)
			ouSet = true;
		Assert.assertTrue(ouSet);
	}

	@Test
	public void createNewObjects() throws DirectoryException {

		final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();

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
		objects.add(location);

		hwtype.setName("h1");
		objects.add(hwtype);

		device.setName("d1");
		objects.add(device);

		user.setName("u1");
		objects.add(user);

		group.setName("g1");
		objects.add(group);

		printer.setName("p1");
		objects.add(printer);

		client.setName("tc1");
		final Set<Device> devices = new HashSet<Device>();
		devices.add(device);
		client.setHardwareType(hwtype);
		client.setLocation(location);
		objects.add(client);

		application.setName("a1");
		objects.add(application);

		appGroup.setName("ag1");
		objects.add(appGroup);

		lcd.setBaseDN("ou=NeueUmgebung");

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		for (final DirectoryObject obj : objects)
			dir.save(obj);

		final Set<DirectoryObject> currentDirObjetSet = new HashSet<DirectoryObject>();

		for (final DirectoryObject o : objects) {
			dir.refresh(o);
			currentDirObjetSet.add(o);
		}

		Assert.assertEquals(objects, currentDirObjetSet);
	}

	@Test
	public void assignObjects() throws DirectoryException {
		final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
		lcd.setBaseDN("ou=NeueUmgebung");

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

		Assert.assertEquals(allMembers, currentMembers);
	}

	@Test
	public void removeAssignements() throws DirectoryException {

		final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
		lcd.setBaseDN("ou=NeueUmgebung");

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
		assignObjects();
	}

	@Test
	public void deleteObjects() throws DirectoryException {
		final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
		lcd.setBaseDN("ou=NeueUmgebung");

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
		final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
		lcd.setBaseDN("ou=NeueUmgebung");

		final Name targetName = Util.makeRelativeName("", lcd);

		Util.deleteRecursively(lcd.createDirContext(), targetName);

		final Set<Realm> realms = LDAPDirectory.listRealms(lcd);

		Assert.assertNull(realms);
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

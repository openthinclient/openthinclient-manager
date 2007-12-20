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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
import org.openthinclient.ldap.DirectoryFacade;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.TypeMapping;
import org.openthinclient.ldap.Util;

@Ignore
// FIXME once it runs
public class TestHybridModelMapping extends AbstractEmbeddedDirectoryTest {
	private static Class[] groupClasses = {UserGroup.class, Application.class,
			ApplicationGroup.class, Printer.class, Device.class};

	private static Class[] objectClasses = {Location.class, UserGroup.class,
			Application.class, ApplicationGroup.class, Printer.class, Device.class,
			HardwareType.class, User.class, Client.class};

	private static String baseDN = "dc=test,dc=test";
	private static String envDN = "ou=NeueUmgebung," + baseDN;

	private LDAPDirectory secondaryDirectory;

	private static String secondBaseDN = "ou=secondaryOU,dc=test,dc=test";

	@Before
	public void createEnvironment() throws Exception {
		Mapping.disableCache = true;

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(baseDN);

		createOU("NeueUmgebung", LDAPDirectory.openEnv(lcd));

		final LDAPConnectionDescriptor lcdNew = getConnectionDescriptor();

		lcdNew.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcdNew);

		final Realm realm = initRealm(dir, "");
		realm.setConnectionDescriptor(lcdNew);

		// Serversettings
		realm.setValue("Serversettings.Hostname", realm.getConnectionDescriptor()
				.getHostname());

		final Short s = new Short(realm.getConnectionDescriptor().getPortNumber());

		realm.setValue("Serversettings.Portnumber", s.toString());
		final String schemaProviderName = realm.getConnectionDescriptor()
				.getHostname();

		realm.setValue("Serversettings.SchemaProviderName", schemaProviderName);

		final LdapContext ctx = lcdNew.createDirectoryFacade().createDirContext();
		initOUs(ctx, dir);

		initAdmin(dir, realm, "administrator", "cn=administrator,ou=users," + envDN); //$NON-NLS-1$

		final Set<Realm> currentRealms = dir.list(Realm.class);

		realm.refresh();

		final Set<OrganizationalUnit> currentOUs = dir
				.list(OrganizationalUnit.class);

		Assert.assertTrue("RealmConfigurations wasn't created!", currentRealms
				.size() > 0);
		Assert.assertTrue("Not all the OUs were created!",
				currentOUs.size() > objectClasses.length + 2);

		Assert.assertNotNull("The group Administrators wasn't created!", realm
				.getAdministrators());
		Assert.assertNotNull("ReadOnlyPrinzipal wasn't created!", realm
				.getReadOnlyPrincipal());

		Assert.assertTrue("No Adminstrator was created!", dir.list(User.class)
				.size() > 0);

		createNewObjects();

		useHybrid();
	}

	@After
	public void destroyEnvironment() throws IOException, DirectoryException,
			NamingException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(baseDN);

		final DirectoryFacade facade = lcd.createDirectoryFacade();
		final DirContext ctx = facade.createDirContext();
		try {
			Util.deleteRecursively(ctx, facade.makeRelativeName(envDN));
		} finally {
			ctx.close();
		}
	}

	public void createNewObjects() throws DirectoryException,
			InstantiationException, IllegalAccessException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		for (final Class<? extends DirectoryObject> c : objectClasses) {
			final DirectoryObject newInstance = c.newInstance();
			newInstance.setName(c.getSimpleName() + " 1");
			dir.save(newInstance);
		}

		for (final Class clazz : objectClasses)
			Assert.assertTrue("Not all the objects were created!", dir.list(clazz)
					.size() > 0);
	}

	// FIXME
	public LDAPDirectory getSecondaryDirectory() {
		return secondaryDirectory;
	}

	public void setSecondaryDirectory(LDAPDirectory secondaryDirectory) {
		this.secondaryDirectory = secondaryDirectory;
	}

	public void useHybrid() throws DirectoryException, InstantiationException,
			IllegalAccessException {

		// organize hybrid-Modus -> new ou
		final LDAPConnectionDescriptor lcdBase = getConnectionDescriptor();
		lcdBase.setBaseDN(baseDN);
		final LDAPDirectory dirBase = LDAPDirectory.openEnv(lcdBase);

		createOU("secondaryOU", dirBase);

		final LDAPConnectionDescriptor lcdSEC = getConnectionDescriptor();
		lcdSEC.setBaseDN("ou=secondaryOU," + baseDN);
		final LDAPDirectory dirSEC = LDAPDirectory.openEnv(lcdSEC);

		createOU("users", dirSEC);
		createOU("usergroups", dirSEC);

		// organize hybrid-Modus -> normal env with secondaryConnection
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		changeProperties("secondaryOU", lcd.getPortNumber());

		Set<Realm> realms = LDAPDirectory.listRealms(lcd);

		Realm realm = new Realm(lcd);
		for (Realm r : realms) {
			realm = r;

		}

		LDAPDirectory dir = realm.getDirectory();

		setSecondaryDirectory(dir);

		// creat Objects
		Class[] secondaryClasses = {User.class, UserGroup.class};

		for (final Class<? extends DirectoryObject> c : secondaryClasses) {
			final DirectoryObject newInstance = c.newInstance();
			newInstance.setName("Sec_" + c.getSimpleName() + " 1");
			dir.save(newInstance);
		}

		for (final Class clazz : objectClasses)
			Assert.assertTrue("Not all the objects were created!", dir.list(clazz)
					.size() > 0);

		Assert.assertTrue("No user were created!",
				dirSEC.list(User.class).size() > 0);

		Assert.assertTrue("No usergroup were created!", dirSEC
				.list(UserGroup.class).size() > 0);
	}

	private LDAPDirectory getDirectory() throws DirectoryException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();

		System.out.println("*** PORTNUMBER: " + lcd.getPortNumber());

		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);
		return dir;
	}

	@Test
	public void assignLocationToClient() throws DirectoryException {
		final LDAPDirectory dir = getSecondaryDirectory();

		final Set<Location> locs = dir.list(Location.class);
		for (final Location loc : locs)
			if (loc.getName().equals("Location 1"))
				for (final Client client : dir.list(Client.class)) {
					client.setLocation(loc);
					dir.save(client);
					dir.refresh(client);
					Assert.assertNotNull(
							"Location of " + client.getName() + " is false!", client
									.getLocation());
				}

	}

	@Test
	public void assignHardwareTypeToClient() throws DirectoryException {
		final LDAPDirectory dir = getSecondaryDirectory();

		final Set<HardwareType> hds = dir.list(HardwareType.class);
		for (final HardwareType hd : hds)
			if (hd.getName().equals("HardwareType 1"))
				for (final Client client : dir.list(Client.class)) {
					client.setHardwareType(hd);
					dir.save(client);
					dir.refresh(client);
					Assert.assertNotNull("HardwareType of " + client.getName()
							+ " is false!", client.getHardwareType());
				}

	}

	@Test
	public void assignApplicationsToClient() throws DirectoryException {
		final LDAPDirectory dir = getSecondaryDirectory();

		for (final Client client : dir.list(Client.class)) {
			client.setApplications(dir.list(Application.class));
			dir.save(client);

			final Client clientNew = dir.load(Client.class, client.getDn());

			Assert.assertTrue("No Applications at: " + clientNew.getName(), clientNew
					.getApplications().size() > 0);
		}

	}

	@Test
	public void assignApplicationGroupsToClient() throws DirectoryException {
		final LDAPDirectory dir = getSecondaryDirectory();

		for (final Client client : dir.list(Client.class)) {
			client.setApplicationGroups(dir.list(ApplicationGroup.class));
			dir.save(client);

			final Client clientNew = dir.load(Client.class, client.getDn());

			Assert.assertTrue("No ApplicationGroups at: " + clientNew.getName(),
					clientNew.getApplicationGroups().size() > 0);
		}

	}

	@Test
	public void assignDevicesToClient() throws DirectoryException {
		final LDAPDirectory dir = getSecondaryDirectory();

		for (final Client client : dir.list(Client.class)) {
			client.setDevices(dir.list(Device.class));
			dir.save(client);

			final Client clientNew = dir.load(Client.class, client.getDn());

			Assert.assertTrue("No Devices at: " + clientNew.getName(), clientNew
					.getDevices().size() > 0);
		}

	}

	@Test
	public void assignClientsToHardwareType() throws DirectoryException {
		final LDAPDirectory dir = getSecondaryDirectory();

		final Set<DirectoryObject> objects = new HashSet<DirectoryObject>();

		objects.addAll(dir.list(Client.class));

		for (final HardwareType hd : dir.list(HardwareType.class))
			Assert.assertTrue("Not all Clients were assigned to: " + hd.getName(),
					assign(hd, objects, dir));
	}

	@Test
	public void assignUserGroupsToUsers() throws DirectoryException {
		final LDAPDirectory dir = getSecondaryDirectory();

		for (final User user : dir.list(User.class)) {
			user.setUserGroups(dir.list(UserGroup.class));
			dir.save(user);

			final User userNew = dir.load(User.class, user.getDn());

			Assert.assertTrue("No UserGroup at: " + userNew.getName(), userNew
					.getUserGroups().size() > 0);
		}
	}

	// FIXME: in DirectoryFacade baseDNName will be wrong
	@Test
	public void assignApplicationGroupsToUsers() throws DirectoryException {
		final LDAPDirectory dir = getSecondaryDirectory();

		for (final User user : dir.list(User.class)) {
			user.setApplicationGroups(dir.list(ApplicationGroup.class));
			dir.save(user);

			final User userNew = dir.load(User.class, user.getDn());

			Assert.assertTrue("No ApplicationGroups at: " + userNew.getName(),
					userNew.getApplicationGroups().size() > 0);
		}
	}

	@Test
	public void assignApplicationsToUsers() throws DirectoryException {
		final LDAPDirectory dir = getSecondaryDirectory();

		for (final User user : dir.list(User.class)) {
			user.setApplications(dir.list(Application.class));
			dir.save(user);

			final User userNew = dir.load(User.class, user.getDn());

			Assert.assertTrue("No Applications at: " + userNew.getName(), userNew
					.getApplications().size() > 0);
		}
	}

	@Test
	public void assignPrintersToUsers() throws DirectoryException {
		final LDAPDirectory dir = getSecondaryDirectory();

		for (final User user : dir.list(User.class)) {
			user.setPrinters(dir.list(Printer.class));
			dir.save(user);

			final User userNew = dir.load(User.class, user.getDn());

			Assert.assertTrue("No Printers at: " + userNew.getName(), userNew
					.getPrinters().size() > 0);
		}
	}

	@Test
	public void assignAllGroups() throws DirectoryException {
		final LDAPDirectory dir = getSecondaryDirectory();

		for (final Class clazz : groupClasses) {
			final Set<DirectoryObject> groups = dir.list(clazz);

			for (final DirectoryObject o : groups)
				if (o instanceof Group) {
					final Group group = (Group) o;

					final Class[] MEMBER_CLASSES = group.getMemberClasses();
					final Set<DirectoryObject> objects = new HashSet<DirectoryObject>();

					for (final Class memberClazz : MEMBER_CLASSES)
						objects.addAll(dir.list(memberClazz));
					final DirectoryObject currentGroup = (DirectoryObject) group;

					Assert.assertTrue("Wrong assignments at group: "
							+ currentGroup.getName(), assign(currentGroup, objects, dir));
				} else
					Assert.assertTrue("This Object: " + o.getName()
							+ " is not a group!!!: ", false);
		}
	}

	private boolean assign(final DirectoryObject o,
			final Set<DirectoryObject> members,

			final LDAPDirectory dir) throws DirectoryException {

		if (o instanceof Group) {
			final Group group = (Group) o;

			final Set<DirectoryObject> oldMembers = group.getMembers();

			members.addAll(oldMembers);

			group.setMembers(members);
			dir.save(group);
		}

		dir.refresh(o);

		final Group g = (Group) dir.load(o.getClass(), o.getDn());

		final Set<DirectoryObject> currentMembers = g.getMembers();

		if (currentMembers.size() == members.size()) {
			for (final DirectoryObject obj : members)
				currentMembers.remove(obj);
			if (0 == currentMembers.size())
				return true;
		}
		return false;
	}

	@Test
	public void removeAssignements() throws DirectoryException {

		final LDAPDirectory dir = getSecondaryDirectory();

		final Set<Group> groupSet = new HashSet<Group>();

		for (final Class cl : groupClasses)
			for (final Object o : dir.list(cl))
				if (o instanceof Group)
					groupSet.add((Group) o);

		final Set<DirectoryObject> currentMembers = new HashSet<DirectoryObject>();

		for (final Group group : groupSet) {
			group.setMembers(new HashSet<DirectoryObject>());
			dir.save(group);
		}

		for (final Group o : groupSet) {
			dir.refresh(o);
			currentMembers.addAll(o.getMembers());
		}
		Assert.assertTrue("Not all Assignements were deleted!", currentMembers
				.size() == 0);
	}

	@Test
	public void renameObjects() throws DirectoryException {

		assignLocationToClient();
		assignHardwareTypeToClient();
		// assignClientsToHardwareType();
		assignAllGroups();

		final String prefixName = "New_";

		final LDAPDirectory dir = getSecondaryDirectory();

		final Set<DirectoryObject> uniqueMembers = new HashSet<DirectoryObject>();

		for (final Class clazz : groupClasses) {
			final Set<DirectoryObject> groups = dir.list(clazz);

			for (final DirectoryObject obj : groups)
				if (obj instanceof Group) {
					final Group group = (Group) obj;
					uniqueMembers.addAll(group.getMembers());
				}
		}

		for (final Class clazz : objectClasses) {
			final Set<DirectoryObject> objects = dir.list(clazz);

			for (final DirectoryObject o : objects)
				Assert.assertTrue("The object: " + o.getName(), rename(o, prefixName,
						dir));
		}

		Assert.assertTrue("Not all Members were renamed", assureMembers(dir,
				uniqueMembers));

		for (final Client client : dir.list(Client.class)) {
			dir.refresh(client);
			Assert.assertNotNull("Location of " + client.getName() + " is false!",
					client.getLocation());
		}

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		for (final Realm realm : LDAPDirectory.listRealms(lcd)) {
			final Set<User> member = realm.getAdministrators().getMembers();
			Assert.assertTrue("Admin wasn't renamed in RealmConfiguration!", member
					.size() > 0);
		}
	}

	private boolean rename(final DirectoryObject o, final String prefixName,
			final LDAPDirectory dir) throws DirectoryException {

		final String oldName = o.getName();

		o.setName(prefixName + oldName);
		dir.save(o);

		final DirectoryObject currentObject = dir.load(o.getClass(), o.getDn());

		if (currentObject.getName().equals(oldName))
			return false;
		return true;
	}

	private boolean assureMembers(LDAPDirectory dir,
			Set<DirectoryObject> oldMembers) throws DirectoryException {
		final Set<DirectoryObject> currentMembers = new HashSet<DirectoryObject>();

		for (final Class clazz : groupClasses) {
			final Set<DirectoryObject> groups = dir.list(clazz);

			for (final DirectoryObject obj : groups)
				if (obj instanceof Group) {
					final Group group = (Group) obj;
					dir.refresh(group);
					currentMembers.addAll(group.getMembers());
				}
		}
		if (currentMembers.size() == oldMembers.size())
			return true;
		return false;
	}

	@Test
	public void changeAndDeleteAttributes() throws DirectoryException {

		final LDAPDirectory dir = getSecondaryDirectory();

		final Set<User> users = dir.list(User.class);

		for (final User user : users) {
			user.setDescription("JUnit-Test");
			dir.save(user);
		}

		for (final User user : users) {
			dir.refresh(user);
			Assert.assertTrue("Discription wasn't added: " + user.getName(),
					null != user.getDescription());
		}

		for (final User user : users) {
			user.setDescription(null);
			dir.save(user);
		}

		for (final User user : users) {
			dir.refresh(user);
			Assert.assertTrue("Discription wasn't deleted: " + user.getName(),
					null == user.getDescription());
		}
	}

	@Test
	public void deleteObjects() throws DirectoryException {
		final LDAPDirectory dir = getSecondaryDirectory();

		final Set<DirectoryObject> objects = new HashSet<DirectoryObject>();

		for (final Class cl : objectClasses)
			for (final Object o : dir.list(cl))
				if (o instanceof DirectoryObject)
					objects.add((DirectoryObject) o);

		if (objects.size() > 0)
			for (final DirectoryObject obj : objects) {
				dir.delete(obj);
				final Set<DirectoryObject> currentObjects = (Set<DirectoryObject>) dir
						.list(obj.getClass());

				for (final DirectoryObject o : currentObjects)
					Assert.assertTrue("Object: " + obj.getName() + " wasn't deleted!",
							obj != o);

				for (final Class clazz : groupClasses) {
					final Set<DirectoryObject> currentGroups = dir.list(clazz);
					for (final DirectoryObject group : currentGroups)
						if (group instanceof Group) {
							final Group g = (Group) group;
							final Set<DirectoryObject> members = g.getMembers();
							for (final DirectoryObject member : members)
								Assert.assertTrue("The UniqueMember: " + obj.getDn()
										+ " wasn't deleted!", !member.getDn().equals(obj.getDn()));
						}
				}
			}
	}

	// @Test
	public void deleteEnvironment() throws NamingException, DirectoryException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final DirectoryFacade df = lcd.createDirectoryFacade();

		final Name targetName = df.makeRelativeName("");
		final LdapContext ctx = df.createDirContext();
		try {
			Util.deleteRecursively(ctx, targetName);

		} finally {
			final LDAPConnectionDescriptor baseLcd = getConnectionDescriptor();
			baseLcd.setBaseDN(baseDN);

			final Set<Realm> realms = LDAPDirectory.listRealms(baseLcd);

			Assert.assertTrue("Not all environments were deleted!",
					realms.size() == 0);

			ctx.close();
		}
	}

	public void changeProperties(String newFolderName, Short portnumber)
			throws DirectoryException {

		final LDAPDirectory dir = getDirectory();

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final Set<Realm> realms = LDAPDirectory.listRealms(lcd);

		for (final Realm realm : realms) {
			realm.setValue("UserGroupSettings.Type", "NewUsersGroups");

			final String ldapUrl = "ldap://localhost:" + portnumber + "/" + "ou="
					+ newFolderName + ",dc=test,dc=test";
			realm.setValue("Secondary.LDAPURLs", ldapUrl);
			realm.setValue("Secondary.Principal", "uid=admin,ou=system");
			realm.setValue("Secondary.Secret", "secret");

			realm.setValue("UserGroupSettings.DirectoryVersion", "secondary");

			dir.save(realm);
		}

		// for (final Realm realm : realms) {
		// dir.refresh(realm);
		// Assert.assertNotNull("No Properties saved: " + realm.getName()
		// + "-DirectoryVersion", realm.getValue("DirectoryVersion"));
		// Assert.assertNotNull("No Properties saved: " + realm.getName() + "-Type",
		// realm.getValue("Type"));
		// Assert.assertNotNull("No Properties saved: " + realm.getName()
		// + "-LDAPURLs", realm.getValue("LDAPURLs"));
		// Assert.assertNotNull("No Properties saved: " + realm.getName()
		// + "-Principal", realm.getValue("Principal"));
		// Assert.assertNotNull("No Properties saved: " + realm.getName()
		// + "-Secret", realm.getValue("Secret"));
		// }
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

		final UserGroup administrators = realm.getAdministrators();

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

			realm.setReadOnlyPrincipal(roPrincipal);

			realm.setName("RealmConfiguration");

			dir.save(realm, "");

			return realm;
		} catch (final Exception e) {
			throw new DirectoryException("Kann Umgebung nicht erstellen", e); //$NON-NLS-1$
		}
	}

}
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

// FIXME once it runs
public class TestBasicMapping extends AbstractEmbeddedDirectoryTest {
	private static Class[] groupClasses = {UserGroup.class, Application.class,
			ApplicationGroup.class, Printer.class, Device.class};

	private static Class[] objectClasses = {Location.class, UserGroup.class,
			Application.class, ApplicationGroup.class, Printer.class, Device.class,
			HardwareType.class, User.class, Client.class};

	private static String baseDN = "dc=test,dc=test";
	private static String envDN = "ou=NeueUmgebung," + baseDN;

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

	@Test
	public void assignToGroupWithRefresh() throws DirectoryException {
		final LDAPDirectory dir = getDirectory();

		final Set<User> users = dir.list(User.class);
		Assert.assertTrue("Doesn't have users", users.size() > 0);

		final Set<UserGroup> userGroups = dir.list(UserGroup.class);
		Assert.assertTrue("Doesn't have groups", userGroups.size() > 0);

		for (final UserGroup group : userGroups) {
			final Set<User> members = group.getMembers();
			Assert.assertEquals("Group not empty", members.size(), 0);

			members.addAll(users);

			dir.save(group);
		}

		for (final UserGroup group : userGroups) {
			dir.refresh(group);

			Assert.assertTrue("Not all Objects were assigned", group.getMembers()
					.containsAll(users));
		}
	}

	@Test
	public void assignToGroupWithReload() throws DirectoryException {
		final LDAPDirectory dir = getDirectory();

		final Set<User> users = dir.list(User.class);
		final Set<UserGroup> userGroups = dir.list(UserGroup.class);

		for (final UserGroup group : userGroups) {
			final Set<User> members = group.getMembers();
			Assert.assertEquals("Group not empty", members.size(), 0);

			members.addAll(users);

			dir.save(group);
		}
	}

	@Test
	public void removeAllFromGroup() throws DirectoryException {
		final LDAPDirectory dir = getDirectory();

		assignToGroupWithRefresh();

		final Set<User> users = dir.list(User.class);
		final Set<UserGroup> userGroups = dir.list(UserGroup.class);

		for (final UserGroup group : userGroups) {
			final Set<User> members = group.getMembers();
			Assert.assertEquals("Group not full", members.size(), users.size());
			members.clear();
			dir.save(group);
		}

		for (UserGroup group : userGroups) {
			group = dir.load(UserGroup.class, group.getDn(), true);
			Assert.assertEquals("Not all Objects were removed", 0, group.getMembers()
					.size());
		}
	}

	@Test
	public void removeOneFromGroup() throws DirectoryException {
		final LDAPDirectory dir = getDirectory();

		assignToGroupWithRefresh();

		final Set<User> users = dir.list(User.class);
		final Set<UserGroup> userGroups = dir.list(UserGroup.class);

		final User toRemove = users.iterator().next();
		users.remove(toRemove);

		for (final UserGroup group : userGroups) {
			final Set<User> members = group.getMembers();
			Assert.assertEquals("Group not full", users.size() + 1, members.size());
			members.remove(toRemove);
			dir.save(group);
		}

		for (UserGroup group : userGroups) {
			group = dir.load(UserGroup.class, group.getDn(), true);
			Assert.assertEquals("Incorrect member count", users.size(), group
					.getMembers().size());
			Assert.assertTrue("Wrong members", group.getMembers().containsAll(users));
		}
	}

	@Test
	public void assignGroupToReferee() throws DirectoryException {
		final LDAPDirectory dir = getDirectory();

		final User user = dir.list(User.class).iterator().next();
		final Set<UserGroup> userGroups = dir.list(UserGroup.class);

		Assert.assertEquals("User has groups", 0, user.getUserGroups().size());

		user.setUserGroups(userGroups);

		dir.save(user);

		dir.refresh(user);

		Assert.assertTrue("Not all Groups were assigned", user.getUserGroups()
				.containsAll(userGroups));

		for (final UserGroup userGroup : userGroups) {
			dir.refresh(userGroup);
			Assert.assertTrue("Group doesn't contain user", userGroup.getMembers()
					.contains(user));
		}
	}

	@Test
	public void removeFromReferee() throws DirectoryException {
		final LDAPDirectory dir = getDirectory();

		final User user = dir.list(User.class).iterator().next();
		final UserGroup group = dir.list(UserGroup.class).iterator().next();

		Assert.assertEquals("User has groups", 0, user.getUserGroups().size());
		Assert.assertEquals("Group has users", 0, group.getMembers().size());

		user.getUserGroups().add(group);
		dir.save(user);

		dir.refresh(group);
		dir.refresh(user);

		Assert.assertTrue("User doesn't have group", user.getUserGroups().contains(
				group));
		Assert.assertTrue("Group doesn't have user", group.getMembers().contains(
				user));

		user.getUserGroups().remove(group);
		dir.save(user);

		dir.save(user);

		dir.refresh(group);
		dir.refresh(user);

		Assert.assertFalse("User has group", user.getUserGroups().contains(group));
		Assert.assertFalse("Group has user", group.getMembers().contains(user));
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
		final LDAPDirectory dir = getDirectory();

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
	public void testUpdateProperty() throws Exception {
		final LDAPDirectory dir = getDirectory();

		HardwareType type = new HardwareType();
		type.setName("foo");
		type.setValue("foo.bar", "foo");

		dir.save(type);

		type = dir.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "foo", type.getValue("foo.bar"));
		Assert.assertEquals("Property count", 1, type.getProperties().getMap()
				.size());

		type.setValue("foo.bar", "bar");

		dir.save(type);

		type = dir.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "bar", type.getValue("foo.bar"));
		Assert.assertEquals("Property count", 1, type.getProperties().getMap()
				.size());
	}

	@Test
	public void testAddProperty() throws Exception {
		final LDAPDirectory dir = getDirectory();

		HardwareType type = new HardwareType();
		type.setName("foo");
		type.setValue("foo.bar", "foo");

		dir.save(type);

		type = dir.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "foo", type.getValue("foo.bar"));
		Assert.assertEquals("Property count", 1, type.getProperties().getMap()
				.size());

		type.setValue("foo.baz", "bar");

		dir.save(type);

		type = dir.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "foo", type.getValue("foo.bar"));
		Assert.assertEquals("Property", "bar", type.getValue("foo.baz"));
		Assert.assertEquals("Property count", 2, type.getProperties().getMap()
				.size());
	}

	@Test
	public void testRemoveProperty() throws Exception {
		final LDAPDirectory dir = getDirectory();

		HardwareType type = new HardwareType();
		type.setName("foo");
		type.setValue("foo.bar", "foo");
		type.setValue("foo.baz", "bar");

		dir.save(type);

		type = dir.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "foo", type.getValue("foo.bar"));
		Assert.assertEquals("Property", "bar", type.getValue("foo.baz"));
		Assert.assertEquals("Property count", 2, type.getProperties().getMap()
				.size());

		type.removeValue("foo.bar");

		dir.save(type);

		type = dir.load(HardwareType.class, type.getDn());

		Assert.assertNull("Property", type.getValue("foo.bar"));
		Assert.assertEquals("Property", "bar", type.getValue("foo.baz"));
		Assert.assertEquals("Property count", 1, type.getProperties().getMap()
				.size());
	}

	@Test
	public void assignHardwareTypeToClient() throws DirectoryException {
		final LDAPDirectory dir = getDirectory();

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
	@Deprecated
	public void assignClientsToHardwareType() throws DirectoryException {
		final LDAPDirectory dir = getDirectory();

		final Set<DirectoryObject> objects = new HashSet<DirectoryObject>();

		objects.addAll(dir.list(Client.class));

		for (final HardwareType hd : dir.list(HardwareType.class))
			Assert.assertTrue("Not all Clients were assigned to: " + hd.getName(),
					assign(hd, objects, dir));
	}

	@Test
	public void assignUserGroupsToUsers() throws DirectoryException {
		// FIXME: Testen !!!!
		final LDAPDirectory dir = getDirectory();

		for (final User user : dir.list(User.class)) {
			user.setUserGroups(dir.list(UserGroup.class));

			final User userNew = dir.load(User.class, user.getDn());

			Assert.assertTrue("No UserGroup at: " + userNew.getName(), userNew
					.getUserGroups().size() > 0);
		}
	}

	// FIXME: create more different Methods like User->Usergroup, Usergroup-> User
	// .....

	@Test
	public void assignAllGroups() throws DirectoryException {
		final LDAPDirectory dir = getDirectory();

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

		final LDAPDirectory dir = getDirectory();

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

		final LDAPDirectory dir = getDirectory();

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

		final LDAPDirectory dir = getDirectory();

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

	// FIXME: BUG -> Properties won't be saved
	@Test
	@Deprecated
	public void changeHostnameProperty() throws DirectoryException {

		final LDAPDirectory dir = getDirectory();

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final Set<Realm> realms = LDAPDirectory.listRealms(lcd);

		for (final Realm realm : realms)
			realm.setValue("Hostname", lcd.getHostname());

		for (final Realm realm : realms) {
			dir.refresh(realm);
			Assert.assertNotNull("No Properties saved: " + realm.getName()
					+ "-Hostname", realm.getValue("Hostname"));
		}
	}

	@Test
	public void destroySomething() {
		// FIXME
	}

	@Test
	public void deleteObjects() throws DirectoryException {
		final LDAPDirectory dir = getDirectory();

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

	@Test
	public void useHybrid() throws DirectoryException, InstantiationException,
			IllegalAccessException {

		// organize hybrid-Modus -> new ou
		final LDAPConnectionDescriptor lcdSEC = getConnectionDescriptor();
		lcdSEC.setBaseDN(baseDN);
		final LDAPDirectory dirSEC = LDAPDirectory.openEnv(lcdSEC);

		createOU("secondaryOU", dirSEC);

		changeProperties("secondaryOU");

		// organize hybrid-Modus -> new env
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final Realm realm = new Realm(lcd);
		final LDAPDirectory dir = LDAPDirectory.openRealm(realm);

		// FIXME: baseDN of user or usergroup == baseDN (dc=test,dc=test)

		// creat Objects
		for (final Class<? extends DirectoryObject> c : objectClasses) {
			final DirectoryObject newInstance = c.newInstance();
			newInstance.setName(c.getSimpleName() + " 1");
			dir.save(newInstance);
		}

		for (final Class clazz : objectClasses)
			Assert.assertTrue("Not all the objects were created!", dir.list(clazz)
					.size() > 0);

		Assert.assertTrue("No secondary objects were created!", dirSEC.list(
				DirectoryObject.class).size() > 0);

		assignAllGroups();
		// assignClientsToHardwareType();
		assignHardwareTypeToClient();
		assignLocationToClient();

		removeAssignements();

		deleteObjects();
	}

	// FIXME: BUG -> Properties won't be saved
	@Deprecated
	public void changeProperties(String newFolderName) throws DirectoryException {

		final LDAPDirectory dir = getDirectory();

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final Set<Realm> realms = LDAPDirectory.listRealms(lcd);

		for (final Realm realm : realms) {
			realm.setValue("DirectoryVersion", "secondary");
			realm.setValue("Type", "NewUsersGroups");

			final String ldapUrl = "ldap://localhost:389/" + "ou=" + newFolderName
					+ ",dc=test,dc=test";
			realm.setValue("LDAPURLs", ldapUrl);
			realm.setValue("Principal", "uid=admin,ou=system");
			realm.setValue("Secret", "secret");
			dir.save(realm);
		}

		for (final Realm realm : realms) {
			dir.refresh(realm);
			Assert.assertNotNull("No Properties saved: " + realm.getName()
					+ "-DirectoryVersion", realm.getValue("DirectoryVersion"));
			Assert.assertNotNull("No Properties saved: " + realm.getName() + "-Type",
					realm.getValue("Type"));
			Assert.assertNotNull("No Properties saved: " + realm.getName()
					+ "-LDAPURLs", realm.getValue("LDAPURLs"));
			Assert.assertNotNull("No Properties saved: " + realm.getName()
					+ "-Principal", realm.getValue("Principal"));
			Assert.assertNotNull("No Properties saved: " + realm.getName()
					+ "-Secret", realm.getValue("Secret"));
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
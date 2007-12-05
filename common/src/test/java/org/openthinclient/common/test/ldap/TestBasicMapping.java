package org.openthinclient.common.test.ldap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openide.ErrorManager;
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

	private static String baseDN = "dc=test,dc=test";
	private static String envDN = "ou=NeueUmgebung," + baseDN;

	@Test
	public void createEnvironment() throws IOException, DirectoryException,
			NamingException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(baseDN);

		createOU("NeueUmgebung", LDAPDirectory.openEnv(lcd));

		final LDAPConnectionDescriptor lcdNew = getConnectionDescriptor();

		lcdNew.setBaseDN(envDN);

		final LdapContext ctx = lcdNew.createDirContext();

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

		// dir = LDAPDirectory.openRealm(realm);
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
		client.setLocation(location);
		objects.add(client);

		application.setName("a1");
		objects.add(application);

		appGroup.setName("ag1");
		objects.add(appGroup);

		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		for (final DirectoryObject obj : objects) {
			dir.save(obj);
		}

		for (final Class clazz : objectClasses) {
			Assert.assertTrue("Not all the objects were created!", dir.list(clazz)
					.size() > 0);
		}

	}

	@Test
	public void assignObjects() throws DirectoryException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Set<User> users = dir.list(User.class);

		final Set<Location> locations = dir.list(Location.class);

		final Set<UserGroup> userGroups = dir.list(UserGroup.class);

		final Set<Client> clients = dir.list(Client.class);

		final Set<Application> applications = dir.list(Application.class);

		final Set<ApplicationGroup> applicationGroups = dir
				.list(ApplicationGroup.class);

		final Set<HardwareType> hwtypeGroups = dir.list(HardwareType.class);

		final Set<Printer> printers = dir.list(Printer.class);

		final Set<Device> devices = dir.list(Device.class);

		final Set<DirectoryObject> groupSet = new HashSet<DirectoryObject>();

		final Hashtable<String, Set<String>> memberGroup = new Hashtable<String, Set<String>>();

		for (final UserGroup group : userGroups) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();

			group.setMembers(users);
			members.addAll(users);

			final Set<String> memberNames = new HashSet<String>();
			for (DirectoryObject o : members) {
				memberNames.add(o.getName());
			}

			memberGroup.put(group.getName(), memberNames);

			groupSet.add(group);
		}

		for (final HardwareType group : hwtypeGroups) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();
			group.setMembers(clients);

			members.addAll(clients);

			final Set<String> memberNames = new HashSet<String>();
			for (DirectoryObject o : members) {
				memberNames.add(o.getName());
			}

			memberGroup.put(group.getName(), memberNames);

			groupSet.add(group);
		}

		for (final Device group : devices) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();
			group.setMembers(hwtypeGroups);
			members.addAll(hwtypeGroups);

			final Set<String> memberNames = new HashSet<String>();
			for (DirectoryObject o : members) {
				memberNames.add(o.getName());
			}

			memberGroup.put(group.getName(), memberNames);

			groupSet.add(group);
		}

		for (final Application group : applications) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();

			members.addAll(users);
			members.addAll(userGroups);
			members.addAll(clients);
			members.addAll(applicationGroups);

			group.setMembers(members);

			final Set<String> memberNames = new HashSet<String>();
			for (DirectoryObject o : members) {
				memberNames.add(o.getName());
			}

			memberGroup.put(group.getName(), memberNames);

			groupSet.add(group);
		}

		for (final ApplicationGroup group : applicationGroups) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();

			members.addAll(users);
			members.addAll(userGroups);
			members.addAll(clients);

			group.setMembers(members);

			final Set<String> memberNames = new HashSet<String>();
			for (DirectoryObject o : members) {
				memberNames.add(o.getName());
			}

			memberGroup.put(group.getName(), memberNames);

			groupSet.add(group);
		}

		for (final Printer group : printers) {
			final Set<DirectoryObject> members = new HashSet<DirectoryObject>();

			members.addAll(users);
			members.addAll(userGroups);
			members.addAll(clients);
			members.addAll(locations);

			group.setMembers(members);

			final Set<String> memberNames = new HashSet<String>();
			for (DirectoryObject o : members) {
				memberNames.add(o.getName());
			}

			memberGroup.put(group.getName(), memberNames);

			groupSet.add(group);
		}

		for (final DirectoryObject o : groupSet) {
			dir.save(o);
		}

		for (final DirectoryObject o : groupSet)
			if (o instanceof Group) {
				dir.refresh(o);
				DirectoryObject obj = dir.load(o.getClass(), o.getDn());

				final Group g = (Group) obj;
				final Set<DirectoryObject> currentMembers = g.getMembers();

				final Set<String> currentMemberNames = new HashSet<String>();
				for (DirectoryObject dobj : currentMembers) {
					currentMemberNames.add(dobj.getName());
				}

				final Object[] oldMemberArray = memberGroup.get(obj.getName())
						.toArray();
				final Object[] currentMemberArray = currentMemberNames.toArray();

				Arrays.sort(oldMemberArray);
				Arrays.sort(currentMemberArray);

				boolean isEquals = Arrays.equals(oldMemberArray, currentMemberArray);

				Assert.assertTrue("Not all Objects were assigned: " + o.getName(),
						isEquals);
			}

	}

	@Test
	public void removeAssignements() throws DirectoryException {

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

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
	public void assignObjectsAgain() throws DirectoryException {
		assignObjects();
	}

	@Test
	public void renameObjects() throws DirectoryException {
//		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
//		lcd.setBaseDN(envDN);
//
//		LDAPDirectory dir = LDAPDirectory.openEnv(lcd);
//
//		final Set<User> users = dir.list(User.class);
//
//		final Set<Location> locations = dir.list(Location.class);
//
//		final Set<UserGroup> userGroups = dir.list(UserGroup.class);
//
//		final Set<Client> clients = dir.list(Client.class);
//
//		final Set<Application> applications = dir.list(Application.class);
//
//		final Set<ApplicationGroup> applicationGroups = dir
//				.list(ApplicationGroup.class);
//
//		final Set<HardwareType> hwtypeGroups = dir.list(HardwareType.class);
//
//		final Set<Printer> printers = dir.list(Printer.class);
//
//		final Set<Device> devices = dir.list(Device.class);
//
//		String prefixName = "New_";
//
//		for (User user : users) {
//			user.setName(prefixName + user.getName());
//			dir.save(user);
//		}
//
//		for (Location loc : locations) {
//			loc.setName(prefixName + loc.getName());
//			dir.save(loc);
//		}
//
//		for (UserGroup ug : userGroups) {
//			ug.setName(prefixName + ug.getName());
//			dir.save(ug);
//		}
//
//		for (Client client : clients) {
//			client.setName(prefixName + client.getName());
//			dir.save(client);
//		}
//		
//		for (Application appl : applications) {
//			appl.setName(prefixName + appl.getName());
//			dir.save(appl);
//		}
//		
//		for (ApplicationGroup appl : applicationGroups) {
//			appl.setName(prefixName + appl.getName());
//			dir.save(appl);
//		}
//		
//		for (HardwareType hwt : hwtypeGroups) {
//			hwt.setName(prefixName + hwt.getName());
//			dir.save(hwt);
//		}
//		
//		for (Printer printer : printers) {
//			printer.setName(prefixName + printer.getName());
//			dir.save(printer);
//		}
//		
//		for (Device device : devices) {
//			device.setName(prefixName + device.getName());
//			dir.save(device);
//		}

//	FIXME: delteObjects will go wrong ??? 
//	FIXME: Assert: rename Objects + rename UniqueMember
		
	}

	@Test
	public void changeProperties() throws DirectoryException {

		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Set<DirectoryObject> allObjects = new HashSet<DirectoryObject>();

		// FIXME: Bug => Changed properties won't be saved

	}

	@Test
	public void deleteObjects() throws DirectoryException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);

		final Set<DirectoryObject> objects = new HashSet<DirectoryObject>();

		for (final Class cl : objectClasses)
			for (final Object o : dir.list(cl))
				if (o instanceof DirectoryObject)
					objects.add((DirectoryObject) o);

		if (objects.size() > 0) {
			for (final DirectoryObject obj : objects) {
				dir.delete(obj);
				Set<DirectoryObject> currentObjects = (Set<DirectoryObject>) dir
						.list(obj.getClass());

				for (final DirectoryObject o : currentObjects) {
					Assert.assertTrue("Object: " + obj.getName() + " wasn't deleted!",
							obj != o);
				}

				for (final Class clazz : groupClasses) {
					Set<DirectoryObject> currentGroups = dir.list(clazz);
					for (final DirectoryObject group : currentGroups) {
						if (group instanceof Group) {
							final Group g = (Group) group;
							final Set<DirectoryObject> members = g.getMembers();
							for (final DirectoryObject member : members) {
								Assert.assertTrue("The UniqueMember: " + obj.getDn()
										+ " wasn't deleted!", !member.getDn().equals(obj.getDn()));
							}
						}
					}
				}
			}
		}
	}

	@Test
	public void deleteEnvironment() throws NamingException, DirectoryException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(envDN);

		final Name targetName = Util.makeRelativeName("", lcd);

		final LdapContext ctx = lcd.createDirContext();
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

	// -----------------------------------------------------------------------------------------------

	private void createOU(String newFolderName, LDAPDirectory directory) {
		try {
			final OrganizationalUnit ou = new OrganizationalUnit();
			ou.setName(newFolderName);
			ou.setDescription("openthinclient.org Console"); //$NON-NLS-1$
			directory.save(ou, "");
		} catch (final DirectoryException e1) {
			ErrorManager.getDefault().notify(e1);
		}
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

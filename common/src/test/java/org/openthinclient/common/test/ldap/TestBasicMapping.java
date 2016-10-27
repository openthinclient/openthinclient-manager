package org.openthinclient.common.test.ldap;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Set;

import javax.naming.NameNotFoundException;
import javax.naming.ldap.LdapContext;

import org.junit.Assert;
import org.junit.Test;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.ClientGroup;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.DirectoryFacade;


public class TestBasicMapping extends AbstractEmbeddedDirectoryTest {
	/**
	 * This method creates the testobjects for the following tests.
	 * @throws Exception
	 */
	private void createTestObjects() throws Exception {
		final User u1 = new User();
		u1.setName("jdoe");
		u1.setGivenName("John");
		u1.setSn("Doe");
		mapping.save(u1);

		final User u2 = new User();
		u2.setName("hhirsch");
		u2.setGivenName("Harry");
		u2.setSn("Hirsch");
		mapping.save(u2);

		final Application a1 = new Application();
		a1.setName("app1");
		mapping.save(a1);

		final Application a2 = new Application();
		a2.setName("app2");
		mapping.save(a2);

		final UserGroup g1 = new UserGroup();
		g1.setName("some users");
		mapping.save(g1);

		final ApplicationGroup ag1 = new ApplicationGroup();
		ag1.setName("some applications");
		mapping.save(ag1);

		final ApplicationGroup ag2 = new ApplicationGroup();
		ag2.setName("some more applications");
		mapping.save(ag2);

		final Location l1 = new Location();
		l1.setName("here");
		mapping.save(l1);

		final Location l2 = new Location();
		l2.setName("there");
		mapping.save(l2);
		// new clientgroup-testobject filled with some clients.
		final ClientGroup cg1 = new ClientGroup();
		cg1.setName("some clients");
		mapping.save(cg1);
		// new clientgroup-testobject filled with some clients.
		final ClientGroup cg2 = new ClientGroup();
		cg2.setName("some more clients");
		mapping.save(cg2);
		
		
	}

	@Test
	public void basicObjectProperties() throws DirectoryException {
		User u = new User();

		u.setName("someName");
		u.setDescription("some description");
		u.setGivenName("John");
		u.setSn("Doe");
		u.setUserPassword(new byte[]{1, 2, 3, 4, 5});

		mapping.save(u);

		// re-load the user
		u = mapping.load(User.class, u.getDn());
		Assert.assertNull("Location", u.getLocation());
		Assert.assertEquals("Name", "someName", u.getName());
		Assert.assertEquals("Description", "some description", u.getDescription());
		Assert.assertEquals("GivenName", "John", u.getGivenName());
		Assert.assertEquals("SN", "Doe", u.getSn());
		Assert.assertArrayEquals("password", new byte[]{1, 2, 3, 4, 5}, u
				.getUserPassword());

		// check refresh as well
		mapping.refresh(u);
		Assert.assertNull("Location", u.getLocation());
		Assert.assertEquals("Name", "someName", u.getName());
		Assert.assertEquals("Description", "some description", u.getDescription());
		Assert.assertEquals("GivenName", "John", u.getGivenName());
		Assert.assertEquals("SN", "Doe", u.getSn());
		Assert.assertArrayEquals("password", new byte[]{1, 2, 3, 4, 5}, u
				.getUserPassword());
	}

	/**
	 * This method tests the groupmapping by adding new clients into a 
	 * new created clientgroup. It saves and loads them by using the default mappers.
	 * @throws Exception
	 */
	@Test
	public void testClientGroupMapping() throws Exception {
		
		// create and save new test client
		Client client = new Client();
		client.setName("otc-client-01");
		mapping.save(client);
		
		System.err.println("DN: " + client.getDn());
		
		
		client = mapping.load(Client.class, client.getDn());
		assertEquals("otc-client-01", client.getName());
		
		//create new clientgroup and add a new member client
		//than save the new group
		ClientGroup group = new ClientGroup();
		group.setName("Funny Client Group");
		group.getMembers().add(client);
		mapping.save(group);
		
		//load the new group
		group = mapping.load(ClientGroup.class, group.getDn());
		assertEquals("Funny Client Group", group.getName());
		assertEquals(1, group.getMembers().size());
		assertEquals("otc-client-01", ((DirectoryObject) group.getMembers().iterator().next()).getName());
		
	}
	
	/**
	 * This method tests the deleting-all-function. It deletes all clients from the clientgroup.
	 * @throws Exception
	 */
	@Test
	public void testDeleteAllClientFromClientGroup() throws Exception {
	
		//First creat some Clients
		Client client = new Client();
		client.setName("otc-client-01");
		mapping.save(client);
		Client client2 = new Client();
		client.setName("otc-client-02");
		mapping.save(client);
		
		//Add them to a new clientgroup
		ClientGroup group = new ClientGroup();
		group.setName("Funny Client Group");
		group.getMembers().add(client);
		mapping.save(group);
		
		//Now delete them all
		final Set<Client> clients = mapping.list(Client.class);
		final Set<ClientGroup> clientGroups = mapping.list(ClientGroup.class);

		for (final ClientGroup groupp : clientGroups) {
			final Set<Client> members = groupp.getMembers();
			Assert.assertEquals("Group not full", members.size(), members.size());
			members.clear();
			mapping.save(groupp);
		}

		for (ClientGroup groupp : clientGroups) {
			groupp = mapping.load(ClientGroup.class, groupp.getDn(), true);
			Assert.assertEquals("Not all Objects were removed", 0, groupp.getMembers()
					.size());
		}
		
	}
	
	/**
	 * This method tests the delete-function for deleting one client from the clientgroup.
	 * @throws Exception
	 */
	@Test
	public void testDeleteOneClientFromClientGroup() throws Exception {
	
		//First creat a Client
		Client client = new Client();
		client.setName("otc-client-01");
		mapping.save(client);

		
		//Add them to a new clientgroup
		ClientGroup group = new ClientGroup();
		group.setName("Funny Client Group");
		group.getMembers().add(client);
		mapping.save(group);
		
		//Now delete one of them
		final Set<Client> clients = mapping.list(Client.class);
		final Set<ClientGroup> clientGroups = mapping.list(ClientGroup.class);

		final Client toRemove = clients.iterator().next();
		clients.remove(toRemove);

		for (final ClientGroup groupp : clientGroups) {
			final Set<Client> members = groupp.getMembers();
			Assert.assertEquals("Group not full", clients.size() + 1, members.size());
			members.remove(toRemove);
			mapping.save(groupp);
		}

		for (ClientGroup groupp : clientGroups) {
			groupp = mapping.load(ClientGroup.class, groupp.getDn(), true);
			Assert.assertEquals("Incorrect member count", clients.size(), groupp
					.getMembers().size());
			Assert.assertTrue("Wrong members", groupp.getMembers().containsAll(clients));
		}
	}
		
	/**
	 * This method tests the adding of one application to many clientgroups.
	 * @throws Exception
	 */
	@Test
	public void testClientGroupAddAppOneToMany() throws Exception {
		final Application a1 = new Application();
		a1.setName("app1");
		mapping.save(a1);

		final Application a2 = new Application();
		a2.setName("app2");
		mapping.save(a2);
		
		final ClientGroup cg1 = new ClientGroup();
		cg1.setName("some clients");
		mapping.save(cg1);
		
		final ClientGroup cg2 = new ClientGroup();
		cg2.setName("some more clients");
		mapping.save(cg2);
		


		final Set<Application> applications = mapping.list(Application.class);
		Assert.assertTrue("Doesn't have applications", applications.size() > 0);

		final Set<ClientGroup> clientGroups = mapping
				.list(ClientGroup.class);
		Assert.assertTrue("Doesn't have groups", clientGroups.size() > 0);


		// add applications to groups
		for (final ClientGroup group : clientGroups) {
			final Set<Application> applicationss = group.getApplications();
			Assert.assertEquals("Group not empty", applicationss.size(), 0);

			applicationss.addAll(applications);

			mapping.save(group);
		}

		for (ClientGroup group : clientGroups) {
			// check with refresh
			mapping.refresh(group);
			Assert.assertTrue("Not all Objects were assigned (refresh)", group
					.getApplications().containsAll(applications));

			// check with reload
			group = mapping.load(ClientGroup.class, group.getDn());
			Assert.assertTrue("Not all Objects were assigned (reload)", group
					.getApplications().containsAll(applications));
		}
	}
	/**
	 * This method tests the function to remove all referenced applications 
	 * from the clientgroup.
	 * @throws Exception
	 */
	@Test
	public void removeAllAppFromClientGroup() throws Exception {
	// delete ALL
		testClientGroupAddAppOneToMany();
	
		final Set<Application> applications = mapping.list(Application.class);
		final Set<ClientGroup> clientGroups = mapping.list(ClientGroup.class);

		for (final ClientGroup group : clientGroups) {
			final Set<Application> members = group.getApplications();
			Assert.assertEquals("Group not full", members.size(), applications.size());
			members.clear();
			mapping.save(group);
		}

		for (ClientGroup group : clientGroups) {
			group = mapping.load(ClientGroup.class, group.getDn(), true);
			Assert.assertEquals("Not all Objects were removed", 0, group.getApplications()
					.size());
		}
	}
	
	/**
	 * This method tests the function to remove one application from the clientgroup.
	 * @throws Exception
	 */
	@Test
	public void removeOneAppFromClientGroup() throws Exception {
	//Delete One
		testClientGroupAddAppOneToMany();
		
		final Set<Application> applications = mapping.list(Application.class);
		final Set<ClientGroup> clientGroups = mapping.list(ClientGroup.class);

		
		final Application toRemove = applications.iterator().next();
		applications.remove(toRemove);

		for (final ClientGroup group : clientGroups) {
			final Set<Application> members = group.getApplications();
			Assert.assertEquals("Group not full", applications.size() + 1, members.size());
			members.remove(toRemove);
			mapping.save(group);
		}

		for (ClientGroup group : clientGroups) {
			group = mapping.load(ClientGroup.class, group.getDn(), true);
			Assert.assertEquals("Incorrect member count", applications.size(), group
					.getApplications().size());
			Assert.assertTrue("Wrong members", group.getApplications().containsAll(applications));
		}
	}
	
	
	@Test
	public void removeObject() throws Exception {
		testUpdateProperty(); // will create tree of stuff

		final Iterator<HardwareType> i = mapping.list(HardwareType.class)
				.iterator();
		final HardwareType t = i.next();

		Assert.assertFalse("too many hardware types found", i.hasNext());

		mapping.delete(t);

		final DirectoryFacade f = connectionDescriptor.createDirectoryFacade();
		final LdapContext ctx = f.createDirContext();
		try {
			ctx.getAttributes(f.makeRelativeName(t.getDn()));
			Assert.fail("object has not been properly deleted");
		} catch (final NameNotFoundException e) {
			// expected
		} finally {
			ctx.close();
		}
	}

	@Test
	public void saveWithFixedDN() throws DirectoryException {
		final User u = new User();

		u.setDn("cn=foobar," + envDN); // doesn't exist!

		try {
			mapping.save(u);
			Assert.fail("Expected exception not thrown");
		} catch (final DirectoryException e) {
			// expected
		}
	}

	@Test
	public void addManyToOne() throws DirectoryException {
		Client c = new Client();
		c.setName("someName");
		c.setDescription("some description");

		final Location l = new Location();
		l.setName("whatever");

		c.setLocation(l);

		mapping.save(c);

		// re-load the user
		c = mapping.load(Client.class, c.getDn());
		Assert.assertEquals("Location", l.getDn(), c.getLocation().getDn());

		// check refresh as well
		mapping.refresh(c);
		Assert.assertEquals("Location", l.getDn(), c.getLocation().getDn());
	}

	@Test
	public void addOneToManyUser() throws Exception {
		createTestObjects();

		final Set<User> users = mapping.list(User.class);
		Assert.assertTrue("Doesn't have users", users.size() > 0);

		final Set<UserGroup> userGroups = mapping.list(UserGroup.class);
		Assert.assertTrue("Doesn't have groups", userGroups.size() > 0);

		// add users to groups
		for (final UserGroup group : userGroups) {
			final Set<User> members = group.getMembers();
			Assert.assertEquals("Group not empty", members.size(), 0);

			members.addAll(users);

			mapping.save(group);
		}

		for (UserGroup group : userGroups) {
			// check with refresh
			mapping.refresh(group);
			Assert.assertTrue("Not all Objects were assigned (refresh)", group
					.getMembers().containsAll(users));

			// check with reload
			group = mapping.load(UserGroup.class, group.getDn());
			Assert.assertTrue("Not all Objects were assigned (reload)", group
					.getMembers().containsAll(users));
		}

		// check inverse ends
		for (User u : users) {
			mapping.refresh(u);
			Assert.assertTrue("User doesn't have all groups (refresh)", u
					.getUserGroups().containsAll(userGroups));

			u = mapping.load(User.class, u.getDn());
			Assert.assertTrue("User doesn't have all groups (reload)", u
					.getUserGroups().containsAll(userGroups));
		}
	}

	@Test
	public void addOneToManyApp() throws Exception {
		createTestObjects();

		final Set<Application> applications = mapping.list(Application.class);
		Assert.assertTrue("Doesn't have applications", applications.size() > 0);

		final Set<ApplicationGroup> applicatonGroups = mapping
				.list(ApplicationGroup.class);
		Assert.assertTrue("Doesn't have groups", applicatonGroups.size() > 0);

		// add applications to groups
		for (final ApplicationGroup group : applicatonGroups) {
			final Set<Application> members = group.getMembers();
			Assert.assertEquals("Group not empty", members.size(), 0);

			members.addAll(applications);

			mapping.save(group);
		}

		for (ApplicationGroup group : applicatonGroups) {
			// check with refresh
			mapping.refresh(group);
			Assert.assertTrue("Not all Objects were assigned (refresh)", group
					.getMembers().containsAll(applications));

			// check with reload
			group = mapping.load(ApplicationGroup.class, group.getDn());
			Assert.assertTrue("Not all Objects were assigned (reload)", group
					.getMembers().containsAll(applications));
		}
	}

	@Test
	public void removeAllFromOneToMany() throws Exception {
		createTestObjects();

		addOneToManyUser();

		final Set<User> users = mapping.list(User.class);
		final Set<UserGroup> userGroups = mapping.list(UserGroup.class);

		for (final UserGroup group : userGroups) {
			final Set<User> members = group.getMembers();
			Assert.assertEquals("Group not full", members.size(), users.size());
			members.clear();
			mapping.save(group);
		}

		for (UserGroup group : userGroups) {
			group = mapping.load(UserGroup.class, group.getDn(), true);
			Assert.assertEquals("Not all Objects were removed", 0, group.getMembers()
					.size());
		}
	}

	@Test
	public void removeOneFromOneToMany() throws Exception {
		createTestObjects();

		addOneToManyUser();

		final Set<User> users = mapping.list(User.class);
		final Set<UserGroup> userGroups = mapping.list(UserGroup.class);

		final User toRemove = users.iterator().next();
		users.remove(toRemove);

		for (final UserGroup group : userGroups) {
			final Set<User> members = group.getMembers();
			Assert.assertEquals("Group not full", users.size() + 1, members.size());
			members.remove(toRemove);
			mapping.save(group);
		}

		for (UserGroup group : userGroups) {
			group = mapping.load(UserGroup.class, group.getDn(), true);
			Assert.assertEquals("Incorrect member count", users.size(), group
					.getMembers().size());
			Assert.assertTrue("Wrong members", group.getMembers().containsAll(users));
		}
	}

	// This test tests a feature to be removed. See SUITE-69
	@Test
	public void addOneToManyInverse() throws Exception {
		createTestObjects();

		final User user = mapping.list(User.class).iterator().next();
		final Set<UserGroup> userGroups = mapping.list(UserGroup.class);

		Assert.assertEquals("User has groups", 0, user.getUserGroups().size());

		user.setUserGroups(userGroups);

		mapping.save(user);

		mapping.refresh(user);

		Assert.assertTrue("Not all Groups were assigned", user.getUserGroups()
				.containsAll(userGroups));

		for (final UserGroup userGroup : userGroups) {
			mapping.refresh(userGroup);
			Assert.assertTrue("Group doesn't contain user", userGroup.getMembers()
					.contains(user));
		}
	}

	// This test tests a feature to be removed. See SUITE-69
	@Test
	public void removeOneToManyInverse() throws Exception {
		createTestObjects();

		final User user = mapping.list(User.class).iterator().next();
		final UserGroup group = mapping.list(UserGroup.class).iterator().next();

		Assert.assertEquals("User has groups", 0, user.getUserGroups().size());
		Assert.assertEquals("Group has users", 0, group.getMembers().size());

		user.getUserGroups().add(group);
		mapping.save(user);

		mapping.refresh(group);
		mapping.refresh(user);

		Assert.assertTrue("User doesn't have group", user.getUserGroups().contains(
				group));
		Assert.assertTrue("Group doesn't have user", group.getMembers().contains(
				user));

		user.getUserGroups().remove(group);
		mapping.save(user);

		mapping.save(user);

		mapping.refresh(group);
		mapping.refresh(user);

		Assert.assertFalse("User has group", user.getUserGroups().contains(group));
		Assert.assertFalse("Group has user", group.getMembers().contains(user));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testUpdateProperty() throws Exception {
		HardwareType type = new HardwareType();
		type.setName("foo");
		type.setValue("foo.bar", "foo");
		type.setValue("foo.bar1", "foo");
		type.setValue("foo.bar2", "foo");
		type.setValue("foo.bar3", "foo");

		mapping.save(type);

		type = mapping.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "foo", type.getValue("foo.bar"));
		Assert.assertEquals("Property count", 4, type.getProperties().getMap()
				.size());

		type.setValue("foo.bar", "bar");

		mapping.save(type);

		type = mapping.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "bar", type.getValue("foo.bar"));
		Assert.assertEquals("Property count", 4, type.getProperties().getMap()
				.size());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testAddProperty() throws Exception {
		HardwareType type = new HardwareType();
		type.setName("foo");
		type.setValue("foo.bar", "foo");

		mapping.save(type);

		type = mapping.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "foo", type.getValue("foo.bar"));
		Assert.assertEquals("Property count", 1, type.getProperties().getMap()
				.size());

		type.setValue("foo.baz", "bar");

		mapping.save(type);

		type = mapping.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "foo", type.getValue("foo.bar"));
		Assert.assertEquals("Property", "bar", type.getValue("foo.baz"));
		Assert.assertEquals("Property count", 2, type.getProperties().getMap()
				.size());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testRemoveProperty() throws Exception {
		HardwareType type = new HardwareType();
		type.setName("foo");
		type.setValue("foo.bar", "foo");
		type.setValue("foo.baz", "bar");

		mapping.save(type);

		type = mapping.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "foo", type.getValue("foo.bar"));
		Assert.assertEquals("Property", "bar", type.getValue("foo.baz"));
		Assert.assertEquals("Property count", 2, type.getProperties().getMap()
				.size());

		type.removeValue("foo.bar");

		mapping.save(type);

		type = mapping.load(HardwareType.class, type.getDn());

		Assert.assertNull("Property", type.getValue("foo.bar"));
		Assert.assertEquals("Property", "bar", type.getValue("foo.baz"));
		Assert.assertEquals("Property count", 1, type.getProperties().getMap()
				.size());
	}

	@Test
	public void clearManyToOneReferencesOnDelete() throws DirectoryException {
		Client c = new Client();
		c.setName("someName");
		c.setDescription("some description");

		final Location l = new Location();
		l.setName("whatever");

		c.setLocation(l);

		mapping.save(c);

		mapping.delete(l);

		// re-load the user
		c = mapping.load(Client.class, c.getDn());
		Assert.assertNull("Location still set", c.getLocation());

		// check refresh as well
		mapping.refresh(c);
		Assert.assertNull("Location still set", c.getLocation());
	}

	@Test
	public void clearOneToManyReferencesOnDelete() throws DirectoryException {
		User u = new User();
		u.setName("hhirsch");

		UserGroup g = new UserGroup();
		g.setName("some other group");

		g.getMembers().add(u);

		mapping.save(g);

		u = mapping.load(User.class, u.getDn());
		Assert.assertTrue("user is in group", u.getUserGroups().contains(g));

		g = mapping.load(UserGroup.class, g.getDn());
		Assert.assertTrue("group has user", g.getMembers().contains(u));

		mapping.delete(u);

		g = mapping.load(UserGroup.class, g.getDn());
		Assert.assertEquals("group still has member", 0, g.getMembers().size());
	}

	@Test
	public void updateManyToOneReferencesOnRename() throws DirectoryException {
		Client c = new Client();
		c.setName("someName");
		c.setDescription("some description");

		final Location l = new Location();
		l.setName("whatever");

		c.setLocation(l);

		mapping.save(c);

		c.setName("someOtherName");

		mapping.save(c);

		// re-load the user
		c = mapping.load(Client.class, c.getDn());
		Assert.assertEquals("Location no longer set", l, c.getLocation());

		// check refresh as well
		mapping.refresh(c);
		Assert.assertEquals("Location no longer set", l, c.getLocation());
	}

	@Test
	public void updateOneToManyReferencesOnRename() throws DirectoryException {
		User u = new User();
		u.setName("hhirsch");

		UserGroup g = new UserGroup();
		g.setName("some other group");

		g.getMembers().add(u);

		mapping.save(g);

		u = mapping.load(User.class, u.getDn());
		Assert.assertTrue("user is in group", u.getUserGroups().contains(g));

		g = mapping.load(UserGroup.class, g.getDn());
		Assert.assertTrue("group has user", g.getMembers().contains(u));

		u.setName("hirschharry");
		mapping.save(u);

		g = mapping.load(UserGroup.class, g.getDn());
		Assert.assertEquals("group no longer has member", 1, g.getMembers().size());
		Assert.assertEquals("group no longer has member", u, g.getMembers()
				.iterator().next());
	}
}
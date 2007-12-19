package org.openthinclient.common.test.ldap;

import java.util.Iterator;
import java.util.Set;

import javax.naming.NameNotFoundException;
import javax.naming.ldap.LdapContext;

import org.junit.Assert;
import org.junit.Test;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.DirectoryFacade;
import org.openthinclient.ldap.Mapping;

public class TestBasicMapping extends AbstractEmbeddedDirectoryTest {
	private Mapping getMapping() throws DirectoryException {
		return mapping;
	}

	private void createTestObjects() throws Exception {
		final Mapping m = getMapping();

		final User u1 = new User();
		u1.setName("jdoe");
		u1.setGivenName("John");
		u1.setSn("Doe");
		m.save(u1);

		final User u2 = new User();
		u2.setName("hhirsch");
		u2.setGivenName("Harry");
		u2.setSn("Hirsch");
		m.save(u2);

		final UserGroup g1 = new UserGroup();
		g1.setName("some users");
		m.save(g1);

		final UserGroup g2 = new UserGroup();
		g2.setName("some users");
		m.save(g2);

		final Location l1 = new Location();
		l1.setName("here");
		m.save(l1);

		final Location l2 = new Location();
		l2.setName("there");
		m.save(l2);
	}

	@Test
	public void basicObjectProperties() throws DirectoryException {
		final Mapping m = getMapping();

		User u = new User();

		u.setName("someName");
		u.setDescription("some description");
		u.setGivenName("John");
		u.setSn("Doe");
		u.setUid(2345);
		u.setUserPassword(new byte[]{1, 2, 3, 4, 5});

		m.save(u);

		// re-load the user
		u = m.load(User.class, u.getDn());
		Assert.assertNull("Location", u.getLocation());
		Assert.assertEquals("Name", "someName", u.getName());
		Assert.assertEquals("Description", "some description", u.getDescription());
		Assert.assertEquals("GivenName", "John", u.getGivenName());
		Assert.assertEquals("SN", "Doe", u.getSn());
		Assert.assertEquals("uid", new Integer(2345), u.getUid());
		Assert.assertArrayEquals("password", new byte[]{1, 2, 3, 4, 5}, u
				.getUserPassword());

		// check refresh as well
		m.refresh(u);
		Assert.assertNull("Location", u.getLocation());
		Assert.assertEquals("Name", "someName", u.getName());
		Assert.assertEquals("Description", "some description", u.getDescription());
		Assert.assertEquals("GivenName", "John", u.getGivenName());
		Assert.assertEquals("SN", "Doe", u.getSn());
		Assert.assertEquals("uid", new Integer(2345), u.getUid());
		Assert.assertArrayEquals("password", new byte[]{1, 2, 3, 4, 5}, u
				.getUserPassword());
	}

	@Test
	public void removeObject() throws Exception {
		final Mapping m = getMapping();

		testUpdateProperty(); // will create tree of stuff

		final Iterator<HardwareType> i = m.list(HardwareType.class).iterator();
		final HardwareType t = i.next();

		Assert.assertFalse("too many hardware types found", i.hasNext());

		m.delete(t);

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
		final Mapping m = getMapping();

		final User u = new User();

		u.setDn("cn=foobar," + envDN); // doesn't exist!

		try {
			m.save(u);
			Assert.fail("Expected exception not thrown");
		} catch (final DirectoryException e) {
			// expected
		}
	}

	@Test
	public void addManyToOne() throws DirectoryException {
		final Mapping m = getMapping();

		Client c = new Client();
		c.setName("someName");
		c.setDescription("some description");

		final Location l = new Location();
		l.setName("whatever");

		c.setLocation(l);

		m.save(c);

		// re-load the user
		c = m.load(Client.class, c.getDn());
		Assert.assertEquals("Location", l.getDn(), c.getLocation().getDn());

		// check refresh as well
		m.refresh(c);
		Assert.assertEquals("Location", l.getDn(), c.getLocation().getDn());
	}

	@Test
	public void addOneToMany() throws Exception {
		createTestObjects();

		final Mapping m = getMapping();

		final Set<User> users = m.list(User.class);
		Assert.assertTrue("Doesn't have users", users.size() > 0);

		final Set<UserGroup> userGroups = m.list(UserGroup.class);
		Assert.assertTrue("Doesn't have groups", userGroups.size() > 0);

		// add users to groups
		for (final UserGroup group : userGroups) {
			final Set<User> members = group.getMembers();
			Assert.assertEquals("Group not empty", members.size(), 0);

			members.addAll(users);

			m.save(group);
		}

		for (UserGroup group : userGroups) {
			// check with refresh
			m.refresh(group);
			Assert.assertTrue("Not all Objects were assigned (refresh)", group
					.getMembers().containsAll(users));

			// check with reload
			group = m.load(UserGroup.class, group.getDn());
			Assert.assertTrue("Not all Objects were assigned (reload)", group
					.getMembers().containsAll(users));
		}

		// check inverse ends
		for (User u : users) {
			m.refresh(u);
			Assert.assertTrue("User doesn't have all groups (refresh)", u
					.getUserGroups().containsAll(userGroups));

			u = m.load(User.class, u.getDn());
			Assert.assertTrue("User doesn't have all groups (reload)", u
					.getUserGroups().containsAll(userGroups));
		}
	}

	@Test
	public void removeAllFromOneToMany() throws Exception {
		createTestObjects();

		final Mapping m = getMapping();

		addOneToMany();

		final Set<User> users = m.list(User.class);
		final Set<UserGroup> userGroups = m.list(UserGroup.class);

		for (final UserGroup group : userGroups) {
			final Set<User> members = group.getMembers();
			Assert.assertEquals("Group not full", members.size(), users.size());
			members.clear();
			m.save(group);
		}

		for (UserGroup group : userGroups) {
			group = m.load(UserGroup.class, group.getDn(), true);
			Assert.assertEquals("Not all Objects were removed", 0, group.getMembers()
					.size());
		}
	}

	@Test
	public void removeOneFromOneToMany() throws Exception {
		createTestObjects();

		final Mapping m = getMapping();

		addOneToMany();

		final Set<User> users = m.list(User.class);
		final Set<UserGroup> userGroups = m.list(UserGroup.class);

		final User toRemove = users.iterator().next();
		users.remove(toRemove);

		for (final UserGroup group : userGroups) {
			final Set<User> members = group.getMembers();
			Assert.assertEquals("Group not full", users.size() + 1, members.size());
			members.remove(toRemove);
			m.save(group);
		}

		for (UserGroup group : userGroups) {
			group = m.load(UserGroup.class, group.getDn(), true);
			Assert.assertEquals("Incorrect member count", users.size(), group
					.getMembers().size());
			Assert.assertTrue("Wrong members", group.getMembers().containsAll(users));
		}
	}

	// This test tests a feature to be removed. See SUITE-69
	@Test
	public void addOneToManyInverse() throws Exception {
		createTestObjects();

		final Mapping m = getMapping();

		final User user = m.list(User.class).iterator().next();
		final Set<UserGroup> userGroups = m.list(UserGroup.class);

		Assert.assertEquals("User has groups", 0, user.getUserGroups().size());

		user.setUserGroups(userGroups);

		m.save(user);

		m.refresh(user);

		Assert.assertTrue("Not all Groups were assigned", user.getUserGroups()
				.containsAll(userGroups));

		for (final UserGroup userGroup : userGroups) {
			m.refresh(userGroup);
			Assert.assertTrue("Group doesn't contain user", userGroup.getMembers()
					.contains(user));
		}
	}

	// This test tests a feature to be removed. See SUITE-69
	@Test
	public void removeOneToManyInverse() throws Exception {
		createTestObjects();

		final Mapping m = getMapping();

		final User user = m.list(User.class).iterator().next();
		final UserGroup group = m.list(UserGroup.class).iterator().next();

		Assert.assertEquals("User has groups", 0, user.getUserGroups().size());
		Assert.assertEquals("Group has users", 0, group.getMembers().size());

		user.getUserGroups().add(group);
		m.save(user);

		m.refresh(group);
		m.refresh(user);

		Assert.assertTrue("User doesn't have group", user.getUserGroups().contains(
				group));
		Assert.assertTrue("Group doesn't have user", group.getMembers().contains(
				user));

		user.getUserGroups().remove(group);
		m.save(user);

		m.save(user);

		m.refresh(group);
		m.refresh(user);

		Assert.assertFalse("User has group", user.getUserGroups().contains(group));
		Assert.assertFalse("Group has user", group.getMembers().contains(user));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testUpdateProperty() throws Exception {
		final Mapping m = getMapping();

		HardwareType type = new HardwareType();
		type.setName("foo");
		type.setValue("foo.bar", "foo");

		m.save(type);

		type = m.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "foo", type.getValue("foo.bar"));
		Assert.assertEquals("Property count", 1, type.getProperties().getMap()
				.size());

		type.setValue("foo.bar", "bar");

		m.save(type);

		type = m.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "bar", type.getValue("foo.bar"));
		Assert.assertEquals("Property count", 1, type.getProperties().getMap()
				.size());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testAddProperty() throws Exception {
		final Mapping m = getMapping();

		HardwareType type = new HardwareType();
		type.setName("foo");
		type.setValue("foo.bar", "foo");

		m.save(type);

		type = m.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "foo", type.getValue("foo.bar"));
		Assert.assertEquals("Property count", 1, type.getProperties().getMap()
				.size());

		type.setValue("foo.baz", "bar");

		m.save(type);

		type = m.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "foo", type.getValue("foo.bar"));
		Assert.assertEquals("Property", "bar", type.getValue("foo.baz"));
		Assert.assertEquals("Property count", 2, type.getProperties().getMap()
				.size());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testRemoveProperty() throws Exception {
		final Mapping m = getMapping();

		HardwareType type = new HardwareType();
		type.setName("foo");
		type.setValue("foo.bar", "foo");
		type.setValue("foo.baz", "bar");

		m.save(type);

		type = m.load(HardwareType.class, type.getDn());

		Assert.assertEquals("Property", "foo", type.getValue("foo.bar"));
		Assert.assertEquals("Property", "bar", type.getValue("foo.baz"));
		Assert.assertEquals("Property count", 2, type.getProperties().getMap()
				.size());

		type.removeValue("foo.bar");

		m.save(type);

		type = m.load(HardwareType.class, type.getDn());

		Assert.assertNull("Property", type.getValue("foo.bar"));
		Assert.assertEquals("Property", "bar", type.getValue("foo.baz"));
		Assert.assertEquals("Property count", 1, type.getProperties().getMap()
				.size());
	}

	@Test
	public void clearManyToOneReferencesOnDelete() throws DirectoryException {
		final Mapping m = getMapping();

		Client c = new Client();
		c.setName("someName");
		c.setDescription("some description");

		final Location l = new Location();
		l.setName("whatever");

		c.setLocation(l);

		m.save(c);

		m.delete(l);

		// re-load the user
		c = m.load(Client.class, c.getDn());
		Assert.assertNull("Location still set", c.getLocation());

		// check refresh as well
		m.refresh(c);
		Assert.assertNull("Location still set", c.getLocation());
	}

	@Test
	public void clearOneToManyReferencesOnDelete() throws DirectoryException {
		final Mapping m = getMapping();

		User u = new User();
		u.setName("hhirsch");

		UserGroup g = new UserGroup();
		g.setName("some other group");

		m.save(u);
		m.save(g);

		g.getMembers().add(u);

		m.save(g);

		u = m.load(User.class, u.getDn());
		Assert.assertTrue("user is in group", u.getUserGroups().contains(g));

		g = m.load(UserGroup.class, g.getDn());
		Assert.assertTrue("group has user", g.getMembers().contains(u));

		m.delete(u);

		g = m.load(UserGroup.class, g.getDn());
		Assert.assertEquals("group still has member", 0, g.getMembers().size());
	}
}
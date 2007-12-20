package org.openthinclient.common.test.ldap;

import javax.naming.NameNotFoundException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openthinclient.common.model.User;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Mapping;

public class TestCaching extends AbstractEmbeddedDirectoryTest {
	private static final String OBJECT_NAME = "someName";
	private static final String OBJECT_NAME_IN_CTX = "cn=" + OBJECT_NAME
			+ ",ou=users";

	@Before
	public void setup() {
		Mapping.disableCache = false;
		mapping.close(); // will just clear the cache;
	}

	@Test
	public void testPopulateCacheAtSave() throws Exception {
		User u = new User();
		u.setName(OBJECT_NAME);
		u.setDescription("some description");
		u.setGivenName("John");
		u.setSn("Doe");
		u.setUid(2345);
		u.setUserPassword(new byte[]{1, 2, 3, 4, 5});

		mapping.save(u);

		// now delete the user, by-passing the mapping
		final LdapContext ctx = connectionDescriptor.createDirectoryFacade()
				.createDirContext();
		ctx.unbind(OBJECT_NAME_IN_CTX);
		ctx.close();

		// re-load the user. it should still be present, due to caching
		u = mapping.load(User.class, u.getDn());
		Assert.assertNull("Location", u.getLocation());
		Assert.assertEquals("Name", OBJECT_NAME, u.getName());
		Assert.assertEquals("Description", "some description", u.getDescription());
		Assert.assertEquals("GivenName", "John", u.getGivenName());
		Assert.assertEquals("SN", "Doe", u.getSn());
		Assert.assertEquals("uid", new Integer(2345), u.getUid());
		Assert.assertArrayEquals("password", new byte[]{1, 2, 3, 4, 5}, u
				.getUserPassword());
	}

	@Test
	public void testPopulateCacheAtLoad() throws Exception {
		User u = new User();
		u.setName(OBJECT_NAME);
		u.setDescription("some description");
		u.setGivenName("John");
		u.setSn("Doe");
		u.setUid(2345);
		u.setUserPassword(new byte[]{1, 2, 3, 4, 5});

		mapping.save(u);

		// will clear the cache
		mapping.close();

		// re-load the user. and populate the cache
		u = mapping.load(User.class, u.getDn());
		Assert.assertNull("Location", u.getLocation());
		Assert.assertEquals("Name", OBJECT_NAME, u.getName());
		Assert.assertEquals("Description", "some description", u.getDescription());
		Assert.assertEquals("GivenName", "John", u.getGivenName());
		Assert.assertEquals("SN", "Doe", u.getSn());
		Assert.assertEquals("uid", new Integer(2345), u.getUid());
		Assert.assertArrayEquals("password", new byte[]{1, 2, 3, 4, 5}, u
				.getUserPassword());

		// now delete the user, by-passing the mapping
		final LdapContext ctx = connectionDescriptor.createDirectoryFacade()
				.createDirContext();
		ctx.unbind(OBJECT_NAME_IN_CTX);
		ctx.close();

		// re-load the user. it should still be present, due to caching
		u = mapping.load(User.class, u.getDn());
		Assert.assertNull("Location", u.getLocation());
		Assert.assertEquals("Name", OBJECT_NAME, u.getName());
		Assert.assertEquals("Description", "some description", u.getDescription());
		Assert.assertEquals("GivenName", "John", u.getGivenName());
		Assert.assertEquals("SN", "Doe", u.getSn());
		Assert.assertEquals("uid", new Integer(2345), u.getUid());
		Assert.assertArrayEquals("password", new byte[]{1, 2, 3, 4, 5}, u
				.getUserPassword());
	}

	@Test
	public void testCacheBypassOnLoadUsingDelete() throws Exception {
		final User u = new User();
		u.setName(OBJECT_NAME);
		u.setDescription("some description");
		u.setGivenName("John");
		u.setSn("Doe");
		u.setUid(2345);
		u.setUserPassword(new byte[]{1, 2, 3, 4, 5});

		mapping.save(u);

		final LdapContext ctx = connectionDescriptor.createDirectoryFacade()
				.createDirContext();

		try {
			ctx.getAttributes(OBJECT_NAME_IN_CTX);
		} catch (final NameNotFoundException e) {
			Assert.fail("user wasn't saved");
		}

		// now delete the user, by-passing the mapping
		ctx.unbind(OBJECT_NAME_IN_CTX);
		ctx.close();

		// must fail!
		try {
			mapping.load(User.class, u.getDn(), true);
			Assert.fail("User loaded although it was deleted from the cache");
		} catch (final DirectoryException e) {
			Assert.assertEquals("Wrong cause", NameNotFoundException.class, e
					.getCause().getClass());
		}
	}

	@Test
	public void testCacheBypassOnLoadUsingUpdate() throws Exception {
		final User u = new User();
		u.setName(OBJECT_NAME);
		u.setDescription("some description");
		u.setGivenName("John");
		u.setSn("Doe");
		u.setUid(2345);
		u.setUserPassword(new byte[]{1, 2, 3, 4, 5});

		mapping.save(u);

		final LdapContext ctx = connectionDescriptor.createDirectoryFacade()
				.createDirContext();

		final ModificationItem mi[] = new ModificationItem[]{new ModificationItem(
				DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("description", "test"))};

		// update attribute
		ctx.modifyAttributes(OBJECT_NAME_IN_CTX, mi);
		ctx.close();

		final User reloaded = mapping.load(User.class, u.getDn(), true);
		Assert.assertEquals("Wrong attribute value still in cache", "test",
				reloaded.getDescription());
	}

	@Test
	public void testCacheBypassOnRefreshdUsingDelete() throws Exception {
		final User u = new User();
		u.setName(OBJECT_NAME);
		u.setDescription("some description");
		u.setGivenName("John");
		u.setSn("Doe");
		u.setUid(2345);
		u.setUserPassword(new byte[]{1, 2, 3, 4, 5});

		mapping.save(u);

		final LdapContext ctx = connectionDescriptor.createDirectoryFacade()
				.createDirContext();

		try {
			ctx.getAttributes(OBJECT_NAME_IN_CTX);
		} catch (final NameNotFoundException e) {
			Assert.fail("user wasn't saved");
		}

		// now delete the user, by-passing the mapping
		ctx.unbind(OBJECT_NAME_IN_CTX);
		ctx.close();

		// must fail!
		try {
			mapping.refresh(u);
			Assert.fail("User loaded although it was deleted from the cache");
		} catch (final DirectoryException e) {
			Assert.assertTrue("wrong exception message", e.getMessage().indexOf(
					"object doesn\'t exist") > 0);
		}
	}

	@Test
	public void testCacheBypassOnRefreshUsingUpdate() throws Exception {
		final User u = new User();
		u.setName(OBJECT_NAME);
		u.setDescription("some description");
		u.setGivenName("John");
		u.setSn("Doe");
		u.setUid(2345);
		u.setUserPassword(new byte[]{1, 2, 3, 4, 5});

		mapping.save(u);

		final LdapContext ctx = connectionDescriptor.createDirectoryFacade()
				.createDirContext();

		final ModificationItem mi[] = new ModificationItem[]{new ModificationItem(
				DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("description", "test"))};

		// update attribute
		ctx.modifyAttributes(OBJECT_NAME_IN_CTX, mi);
		ctx.close();

		mapping.refresh(u);
		Assert.assertEquals("Wrong attribute value still in cache", "test", u
				.getDescription());
	}

	@Test
	public void testCacheUpdateOnObjectUpdate() throws Exception {

	}

	@Test
	public void testPurgeOnDelete() throws Exception {

	}
}
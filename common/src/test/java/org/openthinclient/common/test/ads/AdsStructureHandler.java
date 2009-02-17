package org.openthinclient.common.test.ads;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

@Ignore
public class AdsStructureHandler {

	private static final Logger logger = Logger
			.getLogger(AdsStructureHandler.class);

	private final LDAPConnectionDescriptor lcd;
	private final LDAPDirectory dir;

	private int allUsers = 0;
	private int allGroups = 0;

	public AdsStructureHandler(LDAPConnectionDescriptor lcd, LDAPDirectory dir) {
		this.lcd = lcd;
		this.dir = dir;
	}

	public void setTestVariables() {
		try {
			this.allGroups = giveAllGroups().size();
			this.allUsers = giveAllUser().size();
		} catch (final DirectoryException e) {
			e.printStackTrace();
		}
	}

	private User createOneUser(String name) {
		final User user = new User();
		user.setName(name);
		return user;
	}

	private UserGroup createOneUserGroup(String name, Set<User> users) {
		final UserGroup group = new UserGroup();
		group.setName(name);
		group.setMembers(users);
		return group;
	}

	public Set<User> createUsers(int quantity, String name) {
		final Set<User> users = new HashSet<User>();
		for (int i = 1; i <= quantity; i++)
			users.add(createOneUser(createRandomString() + "_" + name + "_" + i));
		return users;
	}

	private String createRandomString() {
		String ret = "";

		int i = 0;
		while (i < 5) {
			ret = ret + createRandomCharacter();
			i++;
		}

		return ret;
	}

	private char createRandomCharacter() {
		final double randomNumberSetup = Math.random() * 26 + 'a';
		return (char) randomNumberSetup;
	}

	public Set<UserGroup> createUserGroups(int quantity, String name,
			Set<User> users) {
		final Set<UserGroup> usergroups = new HashSet<UserGroup>();
		for (int i = 1; i <= quantity; i++)
			usergroups.add(createOneUserGroup((createRandomString() + "_" + name
					+ "_" + i), users));
		return usergroups;
	}

	public void createOneOu(String name, String dn) {

		final OrganizationalUnit ou = new OrganizationalUnit();
		ou.setName(name);

		logger.info("Create ou: " + name);
		save(ou, dn);
	}

	public Set<User> createAllUsers(int quantityUsers, String dn, String name) {

		final Set<User> user = createUsers(quantityUsers, name);

		for (final User u : user) {
			logger.info("Save user after creation: " + u);
			save(u, dn);
		}

		return user;
	}

	private void saveUpperGroups(Set<UserGroup> usergroups, UserGroup group,
			String dn, Set<User> users) {

		group.setUserGroups(giveMeTheOthers(group, usergroups));

		logger
				.info("Save usergroup after allocate this group with all the otherss: "
						+ group);
		Connection.getAdsStructureHandler().save(group, dn);
		testUserGroup(group.getDn(), usergroups, users);

	}

	private void saveUpperGroups(Set<UserGroup> usergroups, String dn,
			Set<User> users) {

		for (final UserGroup g : usergroups) {
			logger.info("Save usergroup after creation: " + g.getName());
			Connection.getAdsStructureHandler().save(g, dn);

		}

		for (final UserGroup g : usergroups) {
			g.setUserGroups(giveMeTheOthers(g, usergroups));

			logger
					.info("Save usergroup after allocate this group with all the others: "
							+ g);
			Connection.getAdsStructureHandler().save(g, dn);
			testUserGroup(g.getDn(), giveMeTheOthers(g, usergroups), users);
		}

	}

	private Set<UserGroup> giveMeTheOthers(UserGroup group,
			Set<UserGroup> usergroups) {
		final Set<UserGroup> others = new HashSet<UserGroup>();
		for (final UserGroup ug : usergroups)
			if (ug != group)
				others.add(ug);
		return others;

	}

	public void createAllGroups(Set<User> users, Menu.AllocateGroupsModus modus,
			int normal, int upper, String dn, String name) {

		switch (modus){
			case anyoneWithAny :
				final Set<UserGroup> group = Connection.getAdsStructureHandler()
						.createUserGroups(normal + upper, name, users);

				saveUpperGroups(group, dn, users);

				break;

			case onlyLowerToUpper :
				final Set<UserGroup> lowerGroups = Connection.getAdsStructureHandler()
						.createUserGroups(normal, "groups", users);
				final Set<UserGroup> upperGroups = Connection.getAdsStructureHandler()
						.createUserGroups(upper, "uppergroups", users);

				for (final UserGroup u : lowerGroups) {
					logger.info("Save usergroup after creation: " + u);
					Connection.getAdsStructureHandler().save(u, dn);
				}

				for (final UserGroup ug : upperGroups)
					saveUpperGroups(lowerGroups, ug, dn, users);

				break;
		}

	}

	public void save(Object o, String dn) {
		try {
			dir.save(o, dn);
		} catch (final DirectoryException e) {
			e.printStackTrace();
		}
	}

	private Set<User> giveAllUser() throws DirectoryException {
		Set<User> users = new HashSet<User>();

		users = dir.list(User.class);

		return users;
	}

	private Set<UserGroup> giveAllGroups() throws DirectoryException {
		Set<UserGroup> groups = new HashSet<UserGroup>();

		groups = dir.list(UserGroup.class);

		return groups;

	}

	private void testCreation(int quantityUser, int quantityUserGroups) {
		try {
			final Set<User> users = giveAllUser();

			final int a = users.size();
			final int b = this.allUsers + quantityUser;

			Assert.assertTrue(users.size() == this.allUsers + quantityUser);

			final Set<UserGroup> groups = giveAllGroups();

			final int c = groups.size();
			final int d = this.allGroups + quantityUserGroups;

			Assert.assertTrue(groups.size() == this.allGroups + quantityUserGroups);

		} catch (final DirectoryException e) {
			e.printStackTrace();
		}

	}

	public void testAll(int quantityUser, int quantityUserGroups) {
		testCreation(quantityUser, quantityUserGroups);
	}

	private void testUserGroup(String groupDN, Set<UserGroup> allocGroups,
			Set<User> members) {
		try {
			final UserGroup ug = dir.load(UserGroup.class, groupDN);

			Assert.assertTrue(ug.getUserGroups().size() == allocGroups.size());
		} catch (final DirectoryException e) {
			e.printStackTrace();
		}
	}
}

package org.openthinclient.common.test.ads;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

public class AdsStructureHandler {

	private static final Logger logger = Logger
			.getLogger(AdsStructureHandler.class);

	private final LDAPConnectionDescriptor lcd;
	private final LDAPDirectory dir;

	public AdsStructureHandler(LDAPConnectionDescriptor lcd, LDAPDirectory dir) {
		this.lcd = lcd;
		this.dir = dir;
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
			users.add(createOneUser(createRandomCharacter() + "_" + name + "_" + i));
		return users;
	}

	private char createRandomCharacter() {
		final double randomNumberSetup = Math.random() * 26 + 'a';
		return (char) randomNumberSetup;

	}

	public Set<UserGroup> createUserGroups(int quantity, String name,
			Set<User> users) {
		final Set<UserGroup> usergroups = new HashSet<UserGroup>();
		for (int i = 1; i <= quantity; i++)
			usergroups.add(createOneUserGroup((createRandomCharacter() + "_" + name
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
			String dn) {

		group.setUserGroups(usergroups);

		logger.info("Save usergroup after creation: " + group);
		Connection.getAdsStructureHandler().save(group, dn);

	}

	private void saveUpperGroups(Set<UserGroup> usergroups, String dn) {

		for (final UserGroup g : usergroups)
			Connection.getAdsStructureHandler().save(g, dn);

		for (final UserGroup g : usergroups) {
			g.setUserGroups(usergroups);

			logger.info("Save usergroup after creation: " + g);
			Connection.getAdsStructureHandler().save(g, dn);
		}

	}

	public void createAllGroups(Set<User> users, Modus.connectionModus modus,
			int normal, int upper, String dn, String name) {

		switch (modus){
			case anyoneWithAny :
				final Set<UserGroup> group = Connection.getAdsStructureHandler()
						.createUserGroups(normal + upper, name, users);

				saveUpperGroups(group, dn);

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
					saveUpperGroups(lowerGroups, ug, dn);

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

}

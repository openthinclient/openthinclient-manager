package org.openthinclient.common.test.ads;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.openthinclient.common.model.User;

public class AdsCreateAOuNet {

	private final int quantityOUs = 4;
	final int quantityUsersPerOU = 10;
	final int normal = 5;
	final int upper = 0;

	private static final Logger logger = Logger.getLogger(AdsCreateAOuNet.class);

	final Modus.connectionModus modus = Modus.connectionModus.anyoneWithAny;

	private Set<User> createAOuNetUser(String name) {
		final Set<User> usersAll = new HashSet<User>();
		final String name1 = "DC=spielwiese";
		Connection.getAdsStructureHandler().createOneOu(name, "");

		int i = 1;
		final String dn = "ou=" + name + "," + name1;
		String name2 = "";
		while (i < quantityOUs) {
			name2 = name + i;
			Connection.getAdsStructureHandler().createOneOu(name2, dn);
			final String userDn = "ou=" + name2 + "," + dn;
			final Set<User> users = Connection.getAdsStructureHandler()
					.createAllUsers(quantityUsersPerOU, userDn, "user" + i);

			for (final User u : users)
				usersAll.add(u);

			i++;
		}
		return usersAll;
	};

	private String createAOuNetUserGroup(String name, Set<User> users) {
		final String name1 = "DC=spielwiese";
		Connection.getAdsStructureHandler().createOneOu(name, "");

		int i = 1;
		final String dn = "ou=" + name + "," + name1;
		String name2 = "";
		while (i < quantityOUs) {
			name2 = name + i;
			Connection.getAdsStructureHandler().createOneOu(name2, dn);
			final String userDn = "ou=" + name2 + "," + dn;
			Connection.getAdsStructureHandler().createAllGroups(users, modus, normal,
					upper, userDn, "groups");
			i++;
		}
		return dn;
	};

	@Test
	public void testAOuTree() {
		logger.info("*** START ***");

		final Set<User> users = createAOuNetUser("users_net");
		final String usergroupsDn = createAOuNetUserGroup("usergroups_net", users);

		logger.info("*** END ***");
	}
}

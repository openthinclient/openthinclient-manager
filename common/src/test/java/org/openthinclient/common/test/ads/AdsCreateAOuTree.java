package org.openthinclient.common.test.ads;

import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.openthinclient.common.model.User;

public class AdsCreateAOuTree {

	private final int heightOfTheTree = 10;
	final int quantityUsers = 100;
	final int normal = 10;
	final int upper = 0;

	private static final Logger logger = Logger.getLogger(AdsCreateAOuTree.class);

	final Modus.connectionModus modus = Modus.connectionModus.anyoneWithAny;

	private String createAOuTree(String name) {
		final String name1 = "DC=spielwiese";
		Connection.getAdsStructureHandler().createOneOu(name, "");

		int i = 1;
		String dn = "ou=" + name + "," + name1;
		String name2 = "";
		while (i < heightOfTheTree) {
			name2 = name + i;
			Connection.getAdsStructureHandler().createOneOu(name2, dn);
			dn = "ou=" + name2 + "," + dn;
			i++;
		}
		return dn;
	};

	@Test
	public void testAOuTree() {
		logger.info("*** START ***");

		final String usergroupsDn = createAOuTree("usergroups_tree");
		final String usersDn = createAOuTree("users_tree");
		final Set<User> users = Connection.getAdsStructureHandler().createAllUsers(
				quantityUsers, usersDn, "user");
		Connection.getAdsStructureHandler().createAllGroups(users, modus, normal,
				upper, usergroupsDn, "groups");

		logger.info("*** END ***");
	}

}

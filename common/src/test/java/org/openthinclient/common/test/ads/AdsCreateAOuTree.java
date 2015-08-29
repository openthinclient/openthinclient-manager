package org.openthinclient.common.test.ads;

import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.openthinclient.common.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class AdsCreateAOuTree {

	private static final Logger logger = LoggerFactory.getLogger(AdsCreateAOuTree.class);

	private String createAOuTree(String name) {
		final String name1 = Menu.getBaseDN();
		Connection.getAdsStructureHandler().createOneOu(name, "");

		int i = 1;
		String dn = "ou=" + name + "," + name1;
		String name2 = "";
		while (i < Menu.getHeightOfTheTree()) {
			name2 = name + i;
			Connection.getAdsStructureHandler().createOneOu(name2, dn);
			dn = "ou=" + name2 + "," + dn;
			i++;
		}
		return dn;
	};

	@Test
	public void testAOuTree() {
		Connection.getAdsStructureHandler().setTestVariables();
		logger.info("*** START ***");

		final String usergroupsDn = createAOuTree("usergroups_"
				+ Menu.getName(this.getClass()));
		final String usersDn = createAOuTree("users_"
				+ Menu.getName(this.getClass()));
		final Set<User> users = Connection.getAdsStructureHandler().createAllUsers(
				Menu.getQuantityUsers(), usersDn, "user");
		Connection.getAdsStructureHandler().createAllGroups(users, Menu.getModus(),
				Menu.getNormal(), Menu.getUpper(), usergroupsDn, "groups");

		logger.info("*** END ***");
		Connection.getAdsStructureHandler().testAll(Menu.getQuantityUsers(),
				Menu.getNormal() + Menu.getUpper());
	}

}

package org.openthinclient.common.test.ads;

import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.openthinclient.common.model.User;

@Ignore
public class AdsCreateADefaultTree extends Connection {

	private static final Logger logger = Logger
			.getLogger(AdsCreateADefaultTree.class);

	private void createNeededOUs() {
		final String name1 = "users_" + Menu.getName(this.getClass());

		final String name2 = "usergroups_" + Menu.getName(this.getClass());

		Connection.getAdsStructureHandler().createOneOu(name1, "");
		Connection.getAdsStructureHandler().createOneOu(name2, "");
	}

	@Test
	public void createLdapStructure() {
		Connection.getAdsStructureHandler().setTestVariables();

		logger.info("*** START ***");
		createNeededOUs();

		final Set<User> users = Connection.getAdsStructureHandler().createAllUsers(
				Menu.getQuantityUsers(), "ou=users_" + Menu.getName(this.getClass()),
				"user");
		Connection.getAdsStructureHandler().createAllGroups(users, Menu.getModus(),
				Menu.getNormal(), Menu.getUpper(),
				"ou=usergroups_" + Menu.getName(this.getClass()), "groups");

		logger.info("*** END ***");

		final String baseDN = Connection.getConnectionDescriptor().getBaseDN();

		Connection.getAdsStructureHandler().testAll(Menu.getQuantityUsers(),
				Menu.getNormal() + Menu.getUpper());
	}
}

package org.openthinclient.common.test.ads;

import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.openthinclient.common.model.User;

public class MakeAdsStructureTest extends Connection {

	// Menu:
	final int quantityUsers = 10;
	final int normal = 2;
	final int upper = 0;

	final Modus.connectionModus modus = Modus.connectionModus.anyoneWithAny;

	public enum connectionModus {
		onlyLowerToUpper, anyoneWithAny;
	}

	private static final Logger logger = Logger
			.getLogger(MakeAdsStructureTest.class);

	private void createNeededOUs() {
		final String name1 = "users_default";

		final String name2 = "usergroups_default";

		Connection.getAdsStructureHandler().createOneOu(name1, "");
		Connection.getAdsStructureHandler().createOneOu(name2, "");
	}

	@Test
	public void createLdapStructure() {

		logger.info("*** START ***");
		createNeededOUs();

		final Set<User> users = Connection.getAdsStructureHandler().createAllUsers(
				quantityUsers, "ou=users_default", "user");
		Connection.getAdsStructureHandler().createAllGroups(users, modus, normal,
				upper, "ou=usergroups_default", "groups");

		logger.info("*** END ***");
	}
}

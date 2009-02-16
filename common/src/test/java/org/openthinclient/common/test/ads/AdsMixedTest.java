package org.openthinclient.common.test.ads;

import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.openthinclient.common.model.User;

public class AdsMixedTest {
	final int quantityUsers = 10;
	final int normal = 5;
	final int upper = 0;

	private static final Logger logger = Logger.getLogger(AdsCreateAOuNet.class);

	final Modus.connectionModus modus = Modus.connectionModus.anyoneWithAny;

	@Test
	public void createMixed() {
		Connection.getAdsStructureHandler().createOneOu("mixed", "");

		logger.info("*** START ***");
		final Set<User> users = Connection.getAdsStructureHandler().createAllUsers(
				quantityUsers, "ou=mixed", "user");

		Connection.getAdsStructureHandler().createAllGroups(users, modus, normal,
				upper, "ou=mixed", "groups");
		logger.info("*** END ***");

	}

}

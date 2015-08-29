package org.openthinclient.common.test.ads;

import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.openthinclient.common.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class AdsMixedOU {

	private static final Logger logger = LoggerFactory.getLogger(AdsCreateAOuNet.class);

	@Test
	public void createMixed() {
		Connection.getAdsStructureHandler().setTestVariables();
		Connection.getAdsStructureHandler().createOneOu(
				Menu.getName(this.getClass()), "");

		logger.info("*** START ***");
		final Set<User> users = Connection.getAdsStructureHandler().createAllUsers(
				Menu.getQuantityUsers(), "ou=" + Menu.getName(this.getClass()), "user");

		Connection.getAdsStructureHandler().createAllGroups(users, Menu.getModus(),
				Menu.getNormal(), Menu.getUpper(),
				"ou=" + Menu.getName(this.getClass()), "groups");
		logger.info("*** END ***");
		Connection.getAdsStructureHandler().testAll(Menu.getQuantityUsers(),
				Menu.getNormal() + Menu.getUpper());
	}

}

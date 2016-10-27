package org.openthinclient.common.test.ads;

import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.openthinclient.common.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class AdsCreateAOuNet {

	private static final Logger logger = LoggerFactory.getLogger(AdsCreateAOuNet.class);

	private Set<User> createAOuNetUser(String name) {
		final Set<User> usersAll = new HashSet<User>();
		final String name1 = Menu.getBaseDN();
		Connection.getAdsStructureHandler().createOneOu(name, "");

		int i = 1;
		final String dn = "ou=" + name + "," + name1;
		String name2 = "";
		while (i <= Menu.getQuantityOUs()) {
			name2 = name + i;
			Connection.getAdsStructureHandler().createOneOu(name2, dn);
			final String userDn = "ou=" + name2 + "," + dn;
			final Set<User> users = Connection.getAdsStructureHandler()
					.createAllUsers(Menu.getQuantityUsers(), userDn, "user" + i);

			for (final User u : users)
				usersAll.add(u);

			i++;
		}
		return usersAll;
	};

	private String createAOuNetUserGroup(String name, Set<User> users) {
		final String name1 = Menu.getBaseDN();
		Connection.getAdsStructureHandler().createOneOu(name, "");

		int i = 1;
		final String dn = "ou=" + name + "," + name1;
		String name2 = "";
		while (i <= Menu.getQuantityOUs()) {
			name2 = name + i;
			Connection.getAdsStructureHandler().createOneOu(name2, dn);
			final String userDn = "ou=" + name2 + "," + dn;
			Connection.getAdsStructureHandler().createAllGroups(users,
					Menu.getModus(), Menu.getNormal(), Menu.getUpper(), userDn, "groups");
			i++;
		}
		return dn;
	};

	@Test
	public void testAOuNet() {
		Connection.getAdsStructureHandler().setTestVariables();
		logger.info("*** START ***");

		final Set<User> users = createAOuNetUser("users_"
				+ Menu.getName(this.getClass()));
		final String usergroupsDn = createAOuNetUserGroup("usergroups_"
				+ Menu.getName(this.getClass()), users);

		logger.info("*** END ***");
		Connection.getAdsStructureHandler().testAll(
				Menu.getQuantityUsers() * Menu.getQuantityOUs(),
				(Menu.getNormal() + Menu.getUpper()) * Menu.getQuantityOUs());
	}
}

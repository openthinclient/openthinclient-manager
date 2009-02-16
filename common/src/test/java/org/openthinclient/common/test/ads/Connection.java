package org.openthinclient.common.test.ads;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;

public class Connection {

	static LDAPConnectionDescriptor lcd;

	static private AdsStructureHandler ash;

	@BeforeClass
	public static void startConnection() {

		lcd = new LDAPConnectionDescriptor();
		lcd.setBaseDN("dc=spielwiese");
		lcd.setPortNumber((short) 389);
		lcd.setHostname("mkw2k3r2ads");
		lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
		final String principal = "administrator@spielwiese";
		final String secret = "l3v1g0";

		// lcd = new LDAPConnectionDescriptor();
		// lcd.setBaseDN("dc=openthinclient,dc=org");
		// lcd.setPortNumber((short) 10389);
		// lcd.setHostname("localhost");
		// lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
		// final String principal = "uid=admin,ou=System";
		// final String secret = "secret";

		lcd.setCallbackHandler(new UsernamePasswordHandler(principal, secret));
		lcd
				.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);

		ash = new AdsStructureHandler(lcd, Connection.getLDAPDirectory());
	}

	@AfterClass
	public static void stopConncetion() {
		// do something
	}

	@Before
	public void testConnection() {
		final LDAPDirectory dir = getLDAPDirectory();
		if (null == dir)
			System.exit(0);
	}

	public static LDAPDirectory getLDAPDirectory() {
		if (null != lcd)
			try {
				final LDAPDirectory dir = LDAPDirectory.openEnv(lcd);
				return dir;
			} catch (final DirectoryException e) {
				e.printStackTrace();
			}
		return null;
	}

	public static LDAPConnectionDescriptor getConnectionDescriptor() {
		if (null != lcd)
			return lcd;
		else
			return new LDAPConnectionDescriptor();
	}

	public static AdsStructureHandler getAdsStructureHandler() {
		if (null == ash)
			startConnection();
		return ash;
	}

}

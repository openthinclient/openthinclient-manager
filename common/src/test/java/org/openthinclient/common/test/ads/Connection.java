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
		lcd = makeLcd();
		ash = new AdsStructureHandler(lcd, Connection.getLDAPDirectory());
	}

	private static LDAPConnectionDescriptor makeLcd() {
		final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
		lcd.setBaseDN(Menu.getBaseDN());
		lcd.setPortNumber(Menu.getPortNumber());
		lcd.setHostname(Menu.getHostname());
		lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
		final String principal = Menu.getUsername();
		final String secret = Menu.getPassword();

		lcd.setCallbackHandler(new UsernamePasswordHandler(principal, secret));
		lcd
				.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
		return lcd;
	}

	public static LDAPDirectory createTemporaryLDAPDirectory(String dn)
			throws DirectoryException {
		final LDAPConnectionDescriptor newLCD = lcd;
		newLCD.setBaseDN(dn);
		return LDAPDirectory.openEnv(newLCD);
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

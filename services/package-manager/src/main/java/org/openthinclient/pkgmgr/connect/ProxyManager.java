package org.openthinclient.pkgmgr.connect;

import java.util.Properties;

//import org.apache.log4j.Logger;

import com.levigo.util.preferences.PreferenceStoreHolder;

public class ProxyManager {

//private static final Logger logger = Logger.getLogger(ProxyManager.class);




	/**
	 * 
	 * @return the Password which is used to pass the proxy
	 */
	public String getPassword() {
//		initPreferenceStrore();
		return PreferenceStoreHolder
				.getPreferenceStoreByName("PackageManager").getPreferenceAsString(
						"proxyPass", "");
	}

	/**
	 * 
	 * @return the Username which is used to pass the proxy
	 */
	public String getUsername() {
		return PreferenceStoreHolder
				.getPreferenceStoreByName("PackageManager").getPreferenceAsString(
						"proxyUser", "");
	}

	/**
	 * !!Not implemented already!! Should make the User able to change the
	 * Password for the Proxy in a more convenient way should be saved @ programRootDirectory +
	 * File.separator + "nfs" + File.separator + "root" + File.separator + "etc" +
	 * File.separator
	 * 
	 * @param clearPassword
	 */
	public void setPassword(String clearPassword) {
		// for the Future
	}

	/**
	 * !!Not implemented already!! Should make the User able to change the Proxy
	 * Username in a more convenient way should be saved @ programRootDirectory +
	 * File.separator + "nfs" + File.separator + "root" + File.separator + "etc" +
	 * File.separator
	 * 
	 * @param userName
	 */
	public void setUsername(String userName) {
		// for the Future
	}

	/**
	 * !!Not implemented already!! Should make the User able to change the
	 * Password and the Proxy Username in a more convenient way should be saved @ programRootDirectory +
	 * File.separator + "nfs" + File.separator + "root" + File.separator + "etc" +
	 * File.separator
	 * 
	 * @param userName
	 * @param clearPassword
	 */
	public void setUsernameAndPassword(String userName, String clearPassword) {
		// should be made in the future...
//		PreferenceStoreHolder.removePreferenceStore("PackageManager");
//		int seedlength = 8;
//		byte[] seed = new byte[seedlength];
//		long seedValue = System.currentTimeMillis();
//		for (int i = 0; i < seedlength; i++) {
//			seedValue <<= 8;
//			seedValue ^= (long) seed[i] & 0xFF;
//		}
		// String savePass = ConfigurationValueCrypter.encrypt(seed, clearPassword,
		// ConfigurationValueCrypter.Mode.ENCRYPT_CBC,
		// ConfigurationValueCrypter.Algorithm.SHA1,
		// ConfigurationValueCrypter.Encoding.Base64_BROKEN, true);
	}

	/**
	 * check if a Proxy is set, set the System Properties
	 * 
	 */
	public void checkForProxy() {
		if (PreferenceStoreHolder.getPreferenceStoreByName("PackageManager") != null
				&& PreferenceStoreHolder
						.getPreferenceStoreByName("PackageManager")
						.getPreferenceAsString("proxyInUse", "false").equalsIgnoreCase(
								"true")) {
			String proxyHost = PreferenceStoreHolder.getPreferenceStoreByName(
					"PackageManager").getPreferenceAsString("proxyHost", "");
			String proxyPort = PreferenceStoreHolder.getPreferenceStoreByName(
					"PackageManager").getPreferenceAsString("proxyPort", "");
			appendProxyToSystemProperty(proxyHost, proxyPort);
		} else {
			System.getProperties().put("proxySet", "false");
		}
	}

	/**
	 * place the Host and Port to the local System Properties
	 * 
	 * @param proxyHost
	 * @param proxyPort
	 */
	private static void appendProxyToSystemProperty(String proxyHost,
			String proxyPort) {
		final Properties systemSettings = System.getProperties();
		systemSettings.put("proxySet", "true");
		systemSettings.put("http.proxyHost", proxyHost);
		systemSettings.put("http.proxyPort", proxyPort);
	}

}

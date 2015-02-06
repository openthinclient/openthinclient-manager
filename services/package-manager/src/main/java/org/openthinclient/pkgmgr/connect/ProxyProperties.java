package org.openthinclient.pkgmgr.connect;

public class ProxyProperties {
	
	private static boolean proxyInUse;
	private static String proxyHost;
	private static String proxyPort;
	private static String proxyAccount;
	private static String proxyPassword;
	
	public ProxyProperties(String proxyHost,String proxyPort,String proxyAccount,String proxyPassword,boolean proxyInUse) {
		ProxyProperties.proxyAccount = proxyAccount;
		ProxyProperties.proxyHost = proxyHost;
		ProxyProperties.proxyPassword = proxyPassword;
		ProxyProperties.proxyPort = proxyPort;
		ProxyProperties.proxyInUse=proxyInUse;
		
	}
	
	public static String getProxyAccount() {
		return proxyAccount;
	}

	public static String getProxyHost() {
		return proxyHost;
	}

	public static String getProxyPassword() {
		return proxyPassword;
	}

	public static String getProxyPort() {
		return proxyPort;
	}

	public static boolean isProxyInUse() {
		return proxyInUse;
	}

}

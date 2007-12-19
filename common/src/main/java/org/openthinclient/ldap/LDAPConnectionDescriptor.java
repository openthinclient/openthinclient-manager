/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.ldap;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.security.auth.callback.CallbackHandler;

/**
 * A simple structure object to hold information about an LDAP connection.
 * 
 * @author levigo
 */
public class LDAPConnectionDescriptor implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The LDAP service provider type to be used.
	 */
	public enum ProviderType {
		/**
		 * Default SUN provider
		 */
		SUN("com.sun.jndi.ldap.LdapCtxFactory"),

		/**
		 * Apache directory server in embedded mode.
		 */
		APACHE_DS_EMBEDDED("org.apache.directory.server.jndi.ServerContextFactory");

		private final String className;

		ProviderType(String className) {
			this.className = className;
		}

		public String getClassName() {
			return className;
		}
	}

	/**
	 * The connection method to be used
	 */
	public enum ConnectionMethod {
		/**
		 * Unencrypted
		 */
		PLAIN,

		/**
		 * Use Secure Socket Layer
		 */
		SSL,

		/**
		 * Use Start TLS
		 */
		START_TLS;
	}

	/**
	 * The authentication method to use
	 */
	public enum AuthenticationMethod {
		/**
		 * Anonymous bind
		 */
		NONE,
		/**
		 * Username/password authentication
		 */
		SIMPLE,
		/**
		 * SASL
		 */
		SASL;
	}

	/**
	 * The DirectoryType describes a directory server implementation.
	 */
	public enum DirectoryType {
		/**
		 * Microsoft Active Directory Windows 2003 R2
		 */
		MS_2003R2(true),
		/**
		 * Microsoft Active Directory + Services For Unix (SFU)
		 */
		MS_SFU(true),
		/**
		 * Generic RFC style directory (OpenLDAP, Apache DS, ...)
		 */
		GENERIC_RFC(false);

		/**
		 * Flag indicating whether the directory implementations uses all upper case
		 * RDN names.
		 */
		private final boolean upperCaseRDNAttributeNames;

		private DirectoryType(boolean upperCaseRDNAttributeNames) {
			this.upperCaseRDNAttributeNames = upperCaseRDNAttributeNames;
		}

		/**
		 * Return whether directories of this type require all RDN attribute names
		 * to be upper case. I.e. <code>CN=foo,DC=bar,DC=baz</code> instead of
		 * <code>cn=foo,dc=bar,dc=baz</code>.
		 * 
		 * @return
		 */
		public boolean requiresUpperCaseRDNAttributeNames() {
			return upperCaseRDNAttributeNames;
		}
	}

	private ProviderType providerType = ProviderType.SUN;

	private ConnectionMethod connectionMethod;

	private AuthenticationMethod authenticationMethod;

	private String hostname;

	private short portNumber;

	private String baseDN = "";

	private CallbackHandler callbackHandler;

	private Hashtable<String, Object> extraEnv = new Hashtable<String, Object>();

	// private String referralPreference = "ignore";
	private final String referralPreference = "follow";

	/**
	 * Flag indicating whether this connection should be read-only. This is
	 * currently only a hint and may or may not be honored by downstream classes.
	 */
	private boolean readOnly;

	/**
	 * Default constructor. Sets a few reasonable defaults.
	 * 
	 * @param connectionDescriptor
	 */
	public LDAPConnectionDescriptor() {
		this.connectionMethod = ConnectionMethod.PLAIN;
		this.authenticationMethod = AuthenticationMethod.NONE;
		this.hostname = "localhost";
		this.portNumber = -1; // getPortNumber() will figure out a default!
		this.baseDN = "";
	}

	/**
	 * Copy constructor
	 * 
	 * @param connectionDescriptor
	 */
	public LDAPConnectionDescriptor(LDAPConnectionDescriptor lcd) {
		this.providerType = lcd.providerType;
		this.connectionMethod = lcd.connectionMethod;
		this.authenticationMethod = lcd.authenticationMethod;
		this.hostname = lcd.hostname;
		this.portNumber = lcd.portNumber;
		this.baseDN = lcd.baseDN;
		this.callbackHandler = lcd.callbackHandler;
		this.extraEnv.putAll(lcd.extraEnv);
		this.readOnly = lcd.readOnly;
	}

	public String getLDAPUrl() {
		switch (providerType){
			case SUN :
			default :
				return (connectionMethod != ConnectionMethod.SSL ? "ldap" : "ldaps")
						+ "://" + hostname + ":" + getPortNumber() + "/" + baseDN;
			case APACHE_DS_EMBEDDED :
				return baseDN;
		}
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		try {
			final Hashtable<String, Object> env = extraEnv;
			int hashCode = 0;
			for (final Enumeration i = env.keys(); i.hasMoreElements();) {
				final Object key = i.nextElement();
				if (null != key)
					hashCode ^= key.hashCode();
				else
					hashCode ^= 98435234;
				if (null != env.get(key))
					hashCode ^= env.get(key).hashCode();
				else
					hashCode ^= 21876381;
			}

			return hashCode ^ portNumber ^ hostname.hashCode()
					^ callbackHandler.hashCode() ^ connectionMethod.hashCode()
					^ authenticationMethod.hashCode();
		} catch (final Exception e) {
			return -9999;
		}
	}

	public void setAuthenticationMethod(AuthenticationMethod authenticationMethod) {
		this.authenticationMethod = authenticationMethod;
	}

	public AuthenticationMethod getAuthenticationMethod() {
		return authenticationMethod;
	}

	public void setBaseDN(String baseDN) {
		this.baseDN = baseDN;
	}

	public String getBaseDN() {
		return baseDN;
	}

	/**
	 * Set the JAAS {@link CallbackHandler} used to supply authentication
	 * information.
	 * 
	 * @param callbackHandler
	 */
	public void setCallbackHandler(CallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	/**
	 * Get the JAAS {@link CallbackHandler}.
	 * 
	 * @return
	 */
	public CallbackHandler getCallbackHandler() {
		return callbackHandler;
	}

	public void setConnectionMethod(ConnectionMethod connectionMethod) {
		this.connectionMethod = connectionMethod;
	}

	public ConnectionMethod getConnectionMethod() {
		return connectionMethod;
	}

	/**
	 * Set extra environment settings to use.
	 * 
	 * @param extraEnv
	 */
	public void setExtraEnv(Hashtable<String, Object> extraEnv) {
		this.extraEnv = extraEnv;
	}

	/**
	 * Get Hashtable of extra environment settings to use.
	 * 
	 * @return
	 */
	public Hashtable<String, Object> getExtraEnv() {
		return extraEnv;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getHostname() {
		return hostname;
	}

	public void setPortNumber(short portNumber) {
		this.portNumber = portNumber;
	}

	public short getPortNumber() {
		if (portNumber > 0)
			return portNumber;

		if (connectionMethod == ConnectionMethod.SSL)
			return 686;
		else
			return 389;
	}

	/**
	 * @return
	 */
	public boolean isBaseDnSet() {
		return getBaseDN() != null && getBaseDN().length() > 0;
	}

	public ProviderType getProviderType() {
		return providerType;
	}

	public void setProviderType(ProviderType providerType) {
		this.providerType = providerType;
	}

	public String getReferralPreference() {
		return referralPreference;
	}

	public DirectoryFacade createDirectoryFacade() {
		return new DirectoryFacade(this);
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isReadOnly() {
		return readOnly;
	}
}

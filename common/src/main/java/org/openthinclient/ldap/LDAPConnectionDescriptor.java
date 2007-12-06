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

	public enum ProviderType {
		SUN("com.sun.jndi.ldap.LdapCtxFactory"), APACHE_DS_EMBEDDED(
				"org.apache.directory.server.jndi.ServerContextFactory");

		private final String className;

		ProviderType(String className) {
			this.className = className;
		}

		public String getClassName() {
			return className;
		}
	}

	public enum ConnectionMethod {
		PLAIN, SSL, START_TLS;
	}

	public enum AuthenticationMethod {
		NONE, SIMPLE, SASL;
	}

	/**
	 * The DirectoryType describes a directory server implementation.
	 */
	public enum DirectoryType {
		MS_2003R2(true), // Microsoft Active Directory Windows 2003 R2
		MS_SFU(true), // Microsoft Active Directory + SFU
		GENERIC_RFC(false),

		// Generic RFC style Directory
		// APACHE_DS, // Apache Directory Server
		// FEDORA, // Fedory DS
		// OPENLDAP, // Open LDAP slapd
		// SUN_ONE // SUN One LDAP Server
		;

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

	private String schemaProviderName = "";

	private CallbackHandler callbackHandler;

	private Hashtable<String, Object> extraEnv = new Hashtable<String, Object>();

	// private String referralPreference = "ignore";
	private final String referralPreference = "follow";

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

		this.schemaProviderName = "";
	}

	/**
	 * Copy constructor
	 * 
	 * @param connectionDescriptor
	 */
	public LDAPConnectionDescriptor(LDAPConnectionDescriptor cd) {
		this.providerType = cd.providerType;
		this.connectionMethod = cd.connectionMethod;
		this.authenticationMethod = cd.authenticationMethod;
		this.hostname = cd.hostname;
		this.portNumber = cd.portNumber;
		this.baseDN = cd.baseDN;
		this.schemaProviderName = cd.schemaProviderName;
		this.callbackHandler = cd.callbackHandler;
		this.extraEnv.putAll(cd.extraEnv);
		// new
		this.schemaProviderName = cd.schemaProviderName;
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
	 * FIXME: doesn't have anything to do with LDAP...
	 * 
	 * @param schemaProviderName
	 */
	@Deprecated
	public void setSchemaProviderName(String schemaProviderName) {
		this.schemaProviderName = schemaProviderName;
	}

	/**
	 * FIXME: doesn't have anything to do with LDAP...
	 * 
	 * @return
	 */
	@Deprecated
	public String getSchemaProviderName() {
		return schemaProviderName;
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

	public void setExtraEnv(Hashtable<String, Object> extraEnv) {
		this.extraEnv = extraEnv;
	}

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
}

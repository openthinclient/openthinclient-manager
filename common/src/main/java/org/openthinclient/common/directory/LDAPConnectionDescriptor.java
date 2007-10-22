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
package org.openthinclient.common.directory;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.apache.log4j.Logger;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.OneToManyMapping;

import com.sun.jndi.ldap.LdapURL;

/**
 * A simple structure object to hold information about an LDAP connection.
 * 
 * @author levigo
 */
public class LDAPConnectionDescriptor implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger
			.getLogger(LDAPConnectionDescriptor.class);

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

	public enum DirectoryType {
		MS_2003R2, // Microsoft Active Directory Windows 2003 R2
		MS_SFU, // Microsoft Active Directory + SFU
		GENERIC_RFC, // Generic RFC style Directory
		// APACHE_DS, // Apache Directory Server
		// FEDORA, // Fedory DS
		// OPENLDAP, // Open LDAP slapd
		// SUN_ONE // SUN One LDAP Server
		;
	}

	private ProviderType providerType = ProviderType.SUN;

	private ConnectionMethod connectionMethod;

	private AuthenticationMethod authenticationMethod;

	private String hostname;

	private short portNumber;

	private String baseDN = "";

	private String schemaProviderName = "";

	private String directoryVersion = "";

	private DirectoryType serverType;

	private CallbackHandler callbackHandler;

	private Hashtable<Object, Object> extraEnv = new Hashtable<Object, Object>();

	// private String referralPreference = "ignore";
	private String referralPreference = "follow";

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
		this.directoryVersion = "";
		try {
			this.serverType = guessServerType();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		this.directoryVersion = cd.directoryVersion;
		this.serverType = cd.serverType;
	}

	public Hashtable<Object, Object> getLDAPEnv() throws NamingException {
		Hashtable<Object, Object> env = new Hashtable<Object, Object>(extraEnv);
		populateDefaultEnv(env);

		switch (connectionMethod){
			case PLAIN :
				env.put(Context.SECURITY_AUTHENTICATION, "none");
				break;
			case SSL :
				env.put(Context.SECURITY_PROTOCOL, "ssl");
				break;
			case START_TLS :
				// not yet...
				// LdapContext ctx = new InitialLdapContext(env, null);
				// StartTlsResponse tls =
				// (StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());
				// SSLSession sess = tls.negotiate();
				throw new IllegalArgumentException("Start TLS not yet supported");
		}

		switch (authenticationMethod){
			case NONE :
				env.put(Context.SECURITY_AUTHENTICATION, "none");
				break;
			case SIMPLE :
				env.put(Context.SECURITY_AUTHENTICATION, "simple");

				NameCallback nc = new NameCallback("Bind DN");
				PasswordCallback pc = new PasswordCallback("Password", false);
				try {
					callbackHandler.handle(new Callback[]{nc, pc});
				} catch (Exception e) {
					throw new NamingException("Can't get authentication information: "
							+ e);
				}

				env.put(Context.SECURITY_PRINCIPAL, nc.getName());
				env.put(Context.SECURITY_CREDENTIALS, new String(pc.getPassword())
						.getBytes());
				break;
			case SASL :
				// not yet...
				throw new IllegalArgumentException("SASL not yet supported");
		}

		env.put(Context.PROVIDER_URL, getLDAPUrl());

		try {
			InetAddress[] hostAddresses = InetAddress.getAllByName(getHostname());
			InetAddress localHost = InetAddress.getLocalHost();
			for (int i = 0; i < hostAddresses.length; i++) {
				if (hostAddresses[i].isLoopbackAddress()
						|| hostAddresses[i].equals(localHost)) {
					env.put(Mapping.PROPERTY_FORCE_SINGLE_THREADED, Boolean.TRUE);
					break;
				}
			}
		} catch (UnknownHostException e) {
			// should not happen
			logger.error(e);
		}

		return env;
	}

	/**
	 * Populate the enviroment with a few default settings.
	 * 
	 * @param env
	 */
	private void populateDefaultEnv(Hashtable<Object, Object> env) {
		env.put(Context.INITIAL_CONTEXT_FACTORY, providerType.getClassName());
		env.put(Context.REFERRAL, referralPreference);
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

	public LdapContext createInitialContext() throws NamingException {
		while (true) {
			try {
				return new InitialLdapContext(getLDAPEnv(), null);
			} catch (AuthenticationException e) {
				if (callbackHandler instanceof CachingCallbackHandler) {
					try {
						((CachingCallbackHandler) callbackHandler).purgeCache();
					} catch (Exception e1) {
						// if this method call failed, we give up instead of
						// retrying.
						throw new NamingException("Authentication with directory failed: "
								+ e1 + " (was: " + e + ")");
					}
					continue;
				}
				throw e;
			}
		}
	}

	/**
	 * FIXME: the schema heuristic is stupid. just query the identifier.
	 * 
	 * @param d
	 * @return
	 * @throws NamingException
	 * @throws MalformedURLException
	 * @throws DirectoryException
	 */
	public DirectoryType guessServerType() throws NamingException {
		// A-DS doesn't support the get schema operation when connecting
		// locally,
		// but we can recongize it by its INITIAL_CONTEXT_FACTORY

		// komischerwiese ist der providerType beim ADS ProviderType.SUN !!!
		// if (providerType == ProviderType.APACHE_DS_EMBEDDED || providerType ==
		// ProviderType.SUN) {
		// return DirectoryType.GENERIC_RFC;
		// }

		if (providerType == ProviderType.APACHE_DS_EMBEDDED) {
			OneToManyMapping.setDUMMY_MEMBER("DC=dummy");
			return DirectoryType.GENERIC_RFC;
		}

		// try to determine the type of server. at this point we distinguish
		// only
		// rfc-style and MS-ADS style.
		// temporarily switch to RootDSE.
		try {
			DirContext ctx = createInitialContext();

			try {
				DirectoryType type = null;

				String ldapScheme = "";
				String ldapEnvironment = ctx.getEnvironment().get(
						"java.naming.provider.url").toString();

				try {
					LdapURL url = new LdapURL(ldapEnvironment);
					ldapScheme = url.getScheme();

				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}

				String ldapURL = ldapScheme + "://" + hostname;

				// Apache DS?
				String vendorName = "";
				Attribute vendorNameAttr = ctx.getAttributes(ldapURL,
						new String[]{"vendorName"}).get("vendorName");
				if (null != vendorNameAttr) {
					vendorName = vendorNameAttr.get().toString();
				}

				if (vendorName.toUpperCase().startsWith("APACHE")) {
					OneToManyMapping.setDUMMY_MEMBER("DC=dummy");
					return DirectoryType.GENERIC_RFC;
				}

				// MS-ADS style?
				Attributes attrs = ctx.getAttributes(ldapURL,
						new String[]{"dsServiceName"});
				String nextattr = "";
				// Get a list of the attributes
				NamingEnumeration enums = attrs.getIDs();
				// Print out each attribute and its values
				while (enums != null && enums.hasMore()) {
					nextattr = (String) enums.next();
				}
				if (attrs.get(nextattr) == null) {
					type = DirectoryType.GENERIC_RFC;

					if (logger.isDebugEnabled())
						logger.debug("This is GENERIC_RFC");

				} else {
					DirContext schema2 = (DirContext) ctx.getSchema("").lookup(
							"ClassDefinition");
					// List the contents of root
					NamingEnumeration bds = schema2.list("");
					boolean[] hasClassesR2 = new boolean[3];
					boolean[] hasClassesSFU = new boolean[4];

					// check Classes
					while (bds.hasMore()) {
						String s = ((NameClassPair) (bds.next())).getName().toString();
						// Classes 2003R2
						if (s.equals("nisMap"))
							hasClassesR2[0] = true;
						if (s.equals("nisObject"))
							hasClassesR2[1] = true;
						if (s.equals("device"))
							hasClassesR2[2] = true;
						// Classes ADS with SFU
						if (s.equals("msSFU30NisMap") || s.equals("msSFUNISMap"))
							hasClassesSFU[0] = true;
						if (s.equals("msSFU30NisObject") || s.equals("msSFUNisObject"))
							hasClassesSFU[1] = true;
						if (s.equals("msSFU30Ieee802Device")
								|| s.equals("msSFUIeee802Device"))
							hasClassesSFU[2] = true;
						if (s.equals("msSFU30IpHost") || s.equals("msSFUIpHost"))
							hasClassesSFU[3] = true;
					}
					if (hasClassesR2[0] == true && hasClassesR2[1] == true
							&& hasClassesR2[2] == true) {
						type = DirectoryType.MS_2003R2;

						if (logger.isDebugEnabled())
							logger.debug("This is an MS ADS - MS_2003R2");

						OneToManyMapping.setDUMMY_MEMBER(dummy(ctx, ldapURL));
					}
					if (hasClassesSFU[0] == true && hasClassesSFU[1] == true
							&& hasClassesSFU[2] == true && hasClassesSFU[3] == true) {
						type = DirectoryType.MS_SFU;

						if (logger.isDebugEnabled())
							logger.debug("This is an MS ADS - MS_SFU");

						OneToManyMapping.setDUMMY_MEMBER(dummy(ctx, ldapURL));
					}
				}
				if (null == type) {
					throw new NamingException("Unrecognized directory server");
				}
				return type;
			} finally {
				ctx.close();
			}
		} finally {
		}
	}

	public String dummy(DirContext ctx, String ldapURL) throws NamingException {
		Attributes dummyMember = ctx.getAttributes(ldapURL,
				new String[]{"rootDomainNamingContext"});
		String nextDummy = "";
		// Get a list of the attributes
		NamingEnumeration enumsD = dummyMember.getIDs();
		// Print out each attribute and its values
		while (enumsD != null && enumsD.hasMore()) {
			nextDummy = (String) enumsD.next();
		}
		String DM = dummyMember.get(nextDummy).get().toString();
		return (DM);
	}

	/**
	 * @param schema
	 * @param sc
	 * @throws NamingException
	 */
	public static boolean hasObjectClass(DirContext schema, String className)
			throws NamingException {
		SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.OBJECT_SCOPE);
		try {
			schema.list("ClassDefinition/" + className);
			return true;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		try {
			Hashtable<Object, Object> env = extraEnv;
			int hashCode = 0;
			for (Enumeration i = env.keys(); i.hasMoreElements();) {
				Object key = i.nextElement();
				if (null != key) {
					hashCode ^= key.hashCode();
				} else {
					hashCode ^= 98435234;
				}
				if (null != env.get(key)) {
					hashCode ^= env.get(key).hashCode();
				} else {
					hashCode ^= 21876381;
				}
			}

			return hashCode ^ portNumber ^ hostname.hashCode()
					^ callbackHandler.hashCode() ^ connectionMethod.hashCode()
					^ authenticationMethod.hashCode();
		} catch (Exception e) {
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

	public void setSchemaProviderName(String schemaProviderName) {
		this.schemaProviderName = schemaProviderName;
	}

	public String getSchemaProviderName() {
		return schemaProviderName;
	}

	public void setCallbackHandler(CallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public CallbackHandler getCallbackHandler() {
		return callbackHandler;
	}

	public void setConnectionMethod(ConnectionMethod connectionMethod) {
		this.connectionMethod = connectionMethod;
	}

	public ConnectionMethod getConnectionMethod() {
		return connectionMethod;
	}

	public void setExtraEnv(Hashtable<Object, Object> extraEnv) {
		this.extraEnv = extraEnv;
	}

	public Hashtable<Object, Object> getExtraEnv() {
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
		if (portNumber > 0) {
			return portNumber;
		}

		if (connectionMethod == ConnectionMethod.SSL) {
			return 686;
		} else {
			return 389;
		}
	}

	public String getReferralPreference() {
		return referralPreference;
	}

	public void setReferralPreference(String referralPreference) {
		this.referralPreference = referralPreference;
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

	public String getDirectoryVersion() {
		return directoryVersion;
	}

	public DirectoryType getServerType() {
		return serverType;
	}

}

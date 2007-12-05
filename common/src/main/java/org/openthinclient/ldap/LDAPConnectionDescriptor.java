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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
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
import org.openthinclient.common.directory.CachingCallbackHandler;

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

	private String directoryVersion = "";

	private CallbackHandler callbackHandler;

	private Hashtable<String, Object> extraEnv = new Hashtable<String, Object>();

	// private String referralPreference = "ignore";
	private final String referralPreference = "follow";

	private boolean isLocked = false;

	private NameParser nameParser;

	private Name baseDNName;

	private DirectoryType directoryType;

	private Hashtable<Object, Object> ldapEnvironment;

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
	}

	public Hashtable<Object, Object> getLDAPEnv() throws NamingException {
		if (null == ldapEnvironment) {
			ldapEnvironment = new Hashtable<Object, Object>(extraEnv);
			populateDefaultEnv(ldapEnvironment);

			switch (connectionMethod){
				case PLAIN :
					ldapEnvironment.put(Context.SECURITY_AUTHENTICATION, "none");
					break;
				case SSL :
					ldapEnvironment.put(Context.SECURITY_PROTOCOL, "ssl");
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
					ldapEnvironment.put(Context.SECURITY_AUTHENTICATION, "none");
					break;
				case SIMPLE :
					ldapEnvironment.put(Context.SECURITY_AUTHENTICATION, "simple");

					final NameCallback nc = new NameCallback("Bind DN");
					final PasswordCallback pc = new PasswordCallback("Password", false);
					try {
						callbackHandler.handle(new Callback[]{nc, pc});
					} catch (final Exception e) {
						throw new NamingException("Can't get authentication information: "
								+ e);
					}

					ldapEnvironment.put(Context.SECURITY_PRINCIPAL, nc.getName());
					ldapEnvironment.put(Context.SECURITY_CREDENTIALS, new String(pc
							.getPassword()).getBytes());
					break;
				case SASL :
					// not yet...
					throw new IllegalArgumentException("SASL not yet supported");
			}

			ldapEnvironment.put(Context.PROVIDER_URL, getLDAPUrl());

			try {
				final InetAddress[] hostAddresses = InetAddress
						.getAllByName(getHostname());
				final InetAddress localHost = InetAddress.getLocalHost();
				for (final InetAddress element : hostAddresses)
					if (element.isLoopbackAddress() || element.equals(localHost)) {
						ldapEnvironment.put(Mapping.PROPERTY_FORCE_SINGLE_THREADED,
								Boolean.TRUE);
						break;
					}
			} catch (final UnknownHostException e) {
				// should not happen
				logger.error(e);
			}
		}

		return ldapEnvironment;
	}

	/**
	 * Populate the environment with a few default settings.
	 * 
	 * @param env
	 */
	private void populateDefaultEnv(Hashtable<Object, Object> env) {
		env.put(Context.INITIAL_CONTEXT_FACTORY, providerType.getClassName());
		env.put(Context.REFERRAL, referralPreference);

		// Enable connection pooling
		env.put("com.sun.jndi.ldap.connect.pool", "true");
		env.put(Context.BATCHSIZE, "100");
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

	private String getLDAPUrlForRootDSE() {
		switch (providerType){
			case SUN :
			default :
				return (connectionMethod != ConnectionMethod.SSL ? "ldap" : "ldaps")
						+ "://" + hostname + ":" + getPortNumber();
			case APACHE_DS_EMBEDDED :
				return "";
		}
	}
	
	public LdapContext createDirContext() throws NamingException {
		while (true)
			try {
				return new InitialLdapContext(getLDAPEnv(), null);
			} catch (final AuthenticationException e) {
				if (callbackHandler instanceof CachingCallbackHandler) {
					try {
						((CachingCallbackHandler) callbackHandler).purgeCache();
					} catch (final Exception e1) {
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
	
	public LdapContext createRootDSEContext() throws NamingException {
		while (true)
			try {
				Hashtable<Object, Object> env = new Hashtable<Object, Object>(getLDAPEnv());
				env.put(Context.PROVIDER_URL, getLDAPUrlForRootDSE());
				
				return new InitialLdapContext(env, null);
			} catch (final AuthenticationException e) {
				if (callbackHandler instanceof CachingCallbackHandler) {
					try {
						((CachingCallbackHandler) callbackHandler).purgeCache();
					} catch (final Exception e1) {
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

	/**
	 * FIXME: the schema heuristic is stupid. just query the identifier.
	 * 
	 * @param d
	 * @return
	 * @throws NamingException
	 * @throws MalformedURLException
	 * @throws DirectoryException
	 */
	public DirectoryType guessDirectoryType() throws NamingException {
		if (null == directoryType)
			if (providerType == ProviderType.APACHE_DS_EMBEDDED) {
				OneToManyMapping.setDUMMY_MEMBER("DC=dummy");
				directoryType = DirectoryType.GENERIC_RFC;
			} else {
				// try to determine the type of server. at this point we distinguish
				// only rfc-style and MS-ADS style. temporarily switch to RootDSE.
				final DirContext ctx = createRootDSEContext();
				try {
					String ldapScheme = "";
					final String ldapEnvironment = ctx.getEnvironment().get(
							"java.naming.provider.url").toString();

					try {
						final LdapURL url = new LdapURL(ldapEnvironment);
						ldapScheme = url.getScheme();
					} catch (final Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}

					final String ldapURL = ""; // getLDAPUrlForRootDSE();

					// Apache DS?
					String vendorName = "";
					final Attribute vendorNameAttr = ctx.getAttributes(ldapURL,
							new String[]{"vendorName"}).get("vendorName");
					if (null != vendorNameAttr)
						vendorName = vendorNameAttr.get().toString();

					if (vendorName.toUpperCase().startsWith("APACHE")) {
						OneToManyMapping.setDUMMY_MEMBER("DC=dummy");
						return DirectoryType.GENERIC_RFC;
					}

					// MS-ADS style?
					final Attributes attrs = ctx.getAttributes(ldapURL,
							new String[]{"dsServiceName"});
					String nextattr = "";
					// Get a list of the attributes
					final NamingEnumeration enums = attrs.getIDs();
					// Print out each attribute and its values
					while (enums != null && enums.hasMore())
						nextattr = (String) enums.next();
					if (attrs.get(nextattr) == null) {
						directoryType = DirectoryType.GENERIC_RFC;

						if (logger.isDebugEnabled())
							logger.debug("This is GENERIC_RFC");
					} else {
						final DirContext schema2 = (DirContext) ctx.getSchema("").lookup(
								"ClassDefinition");
						// List the contents of root
						final NamingEnumeration bds = schema2.list("");
						final boolean[] hasClassesR2 = new boolean[3];
						final boolean[] hasClassesSFU = new boolean[4];

						// check Classes
						while (bds.hasMore()) {
							final String s = ((NameClassPair) bds.next()).getName()
									.toString();
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
							directoryType = DirectoryType.MS_2003R2;

							if (logger.isDebugEnabled())
								logger.debug("This is an MS ADS - MS_2003R2");

							OneToManyMapping.setDUMMY_MEMBER(dummy(ctx, ldapURL));
						}
						if (hasClassesSFU[0] == true && hasClassesSFU[1] == true
								&& hasClassesSFU[2] == true && hasClassesSFU[3] == true) {
							directoryType = DirectoryType.MS_SFU;

							if (logger.isDebugEnabled())
								logger.debug("This is an MS ADS - MS_SFU");

							OneToManyMapping.setDUMMY_MEMBER(dummy(ctx, ldapURL));
						}
					}
					if (null == directoryType)
						throw new NamingException("Unrecognized directory server");
				} finally {
					ctx.close();
				}
			}

		return directoryType;
	}

	public String dummy(DirContext ctx, String ldapURL) throws NamingException {
		final Attributes dummyMember = ctx.getAttributes(ldapURL,
				new String[]{"rootDomainNamingContext"});
		String nextDummy = "";
		// Get a list of the attributes
		final NamingEnumeration enumsD = dummyMember.getIDs();
		// Print out each attribute and its values
		while (enumsD != null && enumsD.hasMore())
			nextDummy = (String) enumsD.next();
		final String DM = dummyMember.get(nextDummy).get().toString();
		return DM;
	}

	/**
	 * @param schema
	 * @param sc
	 * @throws NamingException
	 */
	public static boolean hasObjectClass(DirContext schema, String className)
			throws NamingException {
		final SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.OBJECT_SCOPE);
		try {
			schema.list("ClassDefinition/" + className);
			return true;
		} catch (final NameNotFoundException e) {
			return false;
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
		assertUnlocked();
		this.authenticationMethod = authenticationMethod;
	}

	private void assertUnlocked() {
		if (isLocked)
			throw new IllegalStateException(
					"LDAPConnectionDescriptor is locked and thus no longer mutable.");
	}

	public AuthenticationMethod getAuthenticationMethod() {
		return authenticationMethod;
	}

	public void setBaseDN(String baseDN) {
		assertUnlocked();
		this.baseDN = baseDN;
		clearCache();
	}

	private void clearCache() {
		nameParser = null;
		baseDNName = null;
		directoryType = null;
		ldapEnvironment = null;
	}

	public String getBaseDN() {
		return baseDN;
	}

	// FIXME: doesn't have anything to do with LDAP...
	@Deprecated
	public void setSchemaProviderName(String schemaProviderName) {
		assertUnlocked();
		this.schemaProviderName = schemaProviderName;
		clearCache();
	}

	// FIXME: doesn't have anything to do with LDAP...
	@Deprecated
	public String getSchemaProviderName() {
		return schemaProviderName;
	}

	public void setCallbackHandler(CallbackHandler callbackHandler) {
		assertUnlocked();
		this.callbackHandler = callbackHandler;
		clearCache();
	}

	public CallbackHandler getCallbackHandler() {
		return callbackHandler;
	}

	public void setConnectionMethod(ConnectionMethod connectionMethod) {
		assertUnlocked();
		this.connectionMethod = connectionMethod;
		clearCache();
	}

	public ConnectionMethod getConnectionMethod() {
		return connectionMethod;
	}

	public void setExtraEnv(Hashtable<String, Object> extraEnv) {
		assertUnlocked();
		this.extraEnv = extraEnv;
		clearCache();
	}

	public Hashtable<String, Object> getExtraEnv() {
		return extraEnv;
	}

	public void setHostname(String hostname) {
		assertUnlocked();
		this.hostname = hostname;
		clearCache();
	}

	public String getHostname() {
		return hostname;
	}

	public void setPortNumber(short portNumber) {
		assertUnlocked();
		this.portNumber = portNumber;
		clearCache();
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
		assertUnlocked();
		this.providerType = providerType;
		clearCache();
	}

	public String getDirectoryVersion() {
		return directoryVersion;
	}

	/**
	 * Lock the connection descriptor preventing further modification.
	 */
	public void lock() {
		isLocked = true;
	}

	boolean isLocked() {
		return isLocked;
	}

	/**
	 * Return the {@link NameParser} for the context described by this connection
	 * descriptor.
	 * 
	 * @return
	 * @throws NamingException
	 */
	public NameParser getNameParser() throws NamingException {
		if (null == nameParser) {
			final DirContext ctx = createDirContext();
			try {
				nameParser = ctx.getNameParser("");
			} finally {
				ctx.close();
			}
		}

		return nameParser;
	}

	/**
	 * Get the baseDN for the described context as a {@link Name}
	 * 
	 * @return
	 * @throws NamingException
	 */
	public Name getBaseDNName() throws NamingException {
		if (null == baseDNName)
			baseDNName = getNameParser().parse(baseDN);

		return baseDNName;
	}

	/**
	 * Return whether the specified name resides within the described context.
	 * 
	 * @param name
	 * @return
	 * @throws NamingException
	 */
	public boolean contains(Name name) throws NamingException {
		return name.startsWith(getBaseDNName());
	}
}

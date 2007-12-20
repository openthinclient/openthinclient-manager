package org.openthinclient.ldap;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.apache.log4j.Logger;
import org.openthinclient.ldap.LDAPConnectionDescriptor.ConnectionMethod;
import org.openthinclient.ldap.LDAPConnectionDescriptor.DirectoryType;
import org.openthinclient.ldap.LDAPConnectionDescriptor.ProviderType;
import org.openthinclient.ldap.auth.CachingCallbackHandler;

/**
 * A facade providing useful services around an LDAP directory connection as
 * described by the {@link LDAPConnectionDescriptor} supplied via the
 * constructor.
 * 
 * The {@link DirectoryFacade} is immutable with respect to the connection it
 * uses.
 * 
 * @author levigo
 */
public class DirectoryFacade {
	private static final Logger logger = Logger.getLogger(DirectoryFacade.class);

	private Name baseDNName;
	private DirectoryType directoryType;
	private Hashtable<Object, Object> ldapEnvironment;
	private NameParser nameParser;
	private final LDAPConnectionDescriptor connectionDescriptor;

	private String dummyDN;

	DirectoryFacade(LDAPConnectionDescriptor lcd) {
		// make copy so that changes to the original descriptor don't end up
		// affecting this facade.
		this.connectionDescriptor = new LDAPConnectionDescriptor(lcd);
	}

	/**
	 * Get the baseDN for the described context as a {@link Name}
	 * 
	 * @return
	 * @throws NamingException
	 */
	public Name getBaseDNName() throws NamingException {
		if (null == baseDNName)
			baseDNName = getNameParser().parse(connectionDescriptor.getBaseDN());

		return baseDNName;
	}

	public Hashtable<Object, Object> getLDAPEnv() throws NamingException {
		if (null == ldapEnvironment) {
			final Hashtable<Object, Object> env = new Hashtable<Object, Object>(
					connectionDescriptor.getExtraEnv());
			populateDefaultEnv(env);

			switch (connectionDescriptor.getConnectionMethod()){
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

			switch (connectionDescriptor.getAuthenticationMethod()){
				case NONE :
					env.put(Context.SECURITY_AUTHENTICATION, "none");
					break;
				case SIMPLE :
					env.put(Context.SECURITY_AUTHENTICATION, "simple");

					final NameCallback nc = new NameCallback("Bind DN");
					final PasswordCallback pc = new PasswordCallback("Password", false);
					try {
						connectionDescriptor.getCallbackHandler().handle(
								new Callback[]{nc, pc});
					} catch (final Exception e) {
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

			env.put(Context.PROVIDER_URL, connectionDescriptor.getLDAPUrl());

			try {
				final InetAddress[] hostAddresses = InetAddress
						.getAllByName(connectionDescriptor.getHostname());
				final InetAddress localHost = InetAddress.getLocalHost();
				for (final InetAddress element : hostAddresses)
					if (element.isLoopbackAddress() || element.equals(localHost)) {
						env.put(Mapping.PROPERTY_FORCE_SINGLE_THREADED, Boolean.TRUE);
						break;
					}
			} catch (final UnknownHostException e) {
				// should not happen
				logger.error(e);
			}

			// create unmodifiable copy.
			this.ldapEnvironment = new UnmodifiableHashtable<Object, Object>(env);
		}

		return ldapEnvironment;
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
	 * FIXME: the schema heuristic is, ... well, ... debatable.
	 * 
	 * @param d
	 * @return
	 * @throws NamingException
	 * @throws MalformedURLException
	 * @throws DirectoryException
	 */
	public DirectoryType guessDirectoryType() throws NamingException {
		if (null == directoryType)
			if (connectionDescriptor.getProviderType() == ProviderType.APACHE_DS_EMBEDDED) {
				this.dummyDN = "DC=dummy";
				directoryType = DirectoryType.GENERIC_RFC;
			} else {
				// try to determine the type of server. at this point we distinguish
				// only rfc-style and MS-ADS style. temporarily switch to RootDSE.
				final DirContext ctx = createRootDSEContext();
				try {
					// Apache DS?
					String vendorName = "";
					final Attribute vendorNameAttr = ctx.getAttributes("",
							new String[]{"vendorName"}).get("vendorName");
					if (null != vendorNameAttr)
						vendorName = vendorNameAttr.get().toString();

					if (vendorName.toUpperCase().startsWith("APACHE")) {
						this.dummyDN = "DC=dummy";
						return DirectoryType.GENERIC_RFC;
					}

					// MS-ADS style?
					final Attributes attrs = ctx.getAttributes("",
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

							this.dummyDN = getDummyDN(ctx);
						}
						if (hasClassesSFU[0] == true && hasClassesSFU[1] == true
								&& hasClassesSFU[2] == true && hasClassesSFU[3] == true) {
							directoryType = DirectoryType.MS_SFU;

							if (logger.isDebugEnabled())
								logger.debug("This is an MS ADS - MS_SFU");

							this.dummyDN = getDummyDN(ctx);
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

	public LdapContext createDirContext() throws NamingException {
		while (true)
			try {
				return new InitialLdapContext(getLDAPEnv(), null);
			} catch (final AuthenticationException e) {
				if (connectionDescriptor.getCallbackHandler() instanceof CachingCallbackHandler) {
					try {
						((CachingCallbackHandler) connectionDescriptor.getCallbackHandler())
								.purgeCache();
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

	private LdapContext createRootDSEContext() throws NamingException {
		while (true)
			try {
				final Hashtable<Object, Object> env = new Hashtable<Object, Object>(
						getLDAPEnv());
				env.put(Context.PROVIDER_URL, getLDAPUrlForRootDSE());

				return new InitialLdapContext(env, null);
			} catch (final AuthenticationException e) {
				if (connectionDescriptor.getCallbackHandler() instanceof CachingCallbackHandler) {
					try {
						((CachingCallbackHandler) connectionDescriptor.getCallbackHandler())
								.purgeCache();
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
	 * Populate the environment with a few default settings.
	 * 
	 * @param env
	 */
	private void populateDefaultEnv(Hashtable<Object, Object> env) {
		env.put(Context.INITIAL_CONTEXT_FACTORY, connectionDescriptor
				.getProviderType().getClassName());
		env.put(Context.REFERRAL, connectionDescriptor.getReferralPreference());

		// Enable connection pooling
		env.put("com.sun.jndi.ldap.connect.pool", "true");
		env.put(Context.BATCHSIZE, "100");
	}

	private String getLDAPUrlForRootDSE() {
		switch (connectionDescriptor.getProviderType()){
			case SUN :
			default :
				return (connectionDescriptor.getConnectionMethod() != ConnectionMethod.SSL
						? "ldap"
						: "ldaps")
						+ "://"
						+ connectionDescriptor.getHostname()
						+ ":"
						+ connectionDescriptor.getPortNumber();
			case APACHE_DS_EMBEDDED :
				return "";
		}
	}

	/**
	 * Determine a dummy-DN based on the first ROOT DSE name.
	 * 
	 * @param ctx
	 * @return
	 * @throws NamingException
	 */
	private String getDummyDN(DirContext ctx) throws NamingException {
		final Attributes dummyMember = ctx.getAttributes("",
				new String[]{"rootDomainNamingContext"});
		String nextDummy = "";

		// get the first root DSE name.
		for (final NamingEnumeration e = dummyMember.getIDs(); e != null
				&& e.hasMore();)
			nextDummy = (String) e.next();

		return dummyMember.get(nextDummy).get().toString();
	}

	/**
	 * Adjust the case of the attribute names in the given name according to the
	 * needs of the target directory. E.g. ActiveDirectory wants all upper-case
	 * names.
	 * 
	 * FIXME: making use of the fact that the parsed names are actually
	 * {@link LdapName}s could make this more efficient.
	 * 
	 * @param memberDN
	 * @param connectionDescriptor
	 * @return
	 * @throws NamingException
	 */
	public String fixNameCase(String memberDN) throws NamingException {

		if (!guessDirectoryType().requiresUpperCaseRDNAttributeNames())
			return memberDN;

		// use context's name parser to split the name into parts
		final Name parsed = getNameParser().parse(memberDN);
		Name adjusted = null;
		for (final Enumeration<String> e = parsed.getAll(); e.hasMoreElements();) {
			String part = e.nextElement();

			final int idx = part.indexOf('=');
			final char c[] = part.toCharArray();
			for (int i = 0; i < idx; i++)
				c[i] = Character.toUpperCase(c[i]);
			part = new String(c);

			if (null == adjusted)
				adjusted = getNameParser().parse(part);
			else
				adjusted.add(part);
		}

		return adjusted.toString();
	}

	/**
	 * @param name
	 * @param connectionDescriptor
	 * @return
	 * @throws NamingException
	 * 
	 * FIXME: respect upper case DN requirements
	 */
	public Name makeAbsoluteName(String name) throws NamingException {
		final Name parsedName = getNameParser().parse(name);

		// if the name is relative, append ctx base dn
		if (!contains(parsedName))
			parsedName.addAll(0, getBaseDNName());

		return parsedName;
	}

	/**
	 * @param name
	 * @param connectionDescriptor
	 * @return
	 * @throws NamingException
	 * 
	 * FIXME: respect upper case DN requirements
	 */
	public Name makeRelativeName(String name) throws NamingException {
		final Name parsedName = getNameParser().parse(name);

		// return name directly if it is not absolute
		if (!parsedName.startsWith(getBaseDNName()))
			return parsedName;

		// don't remove suffix, if the connections base DN is zero-sized
		if (getBaseDNName().size() == 0)
			return parsedName;

		return parsedName.getSuffix(getBaseDNName().size());
	}

	/**
	 * Get the dummy member to be used in 1..n attributes for groups when the
	 * group doesn't have a member.
	 * 
	 * @return
	 * @throws NamingException
	 */
	public String getDummyMember() throws NamingException {
		if (null == dummyDN)
			guessDirectoryType(); // this'll initialize it!
		return dummyDN;
	}

	/**
	 * Return whether the given DN is a dummy DN.
	 * 
	 * @return
	 */
	public boolean isDummyMember(String dn) {
		return dummyDN.equalsIgnoreCase(dn);
	}

	public boolean isReadOnly() {
		return connectionDescriptor.isReadOnly();
	}

	public Name makeAbsoluteName(Name name) throws InvalidNameException {
		// is name already absolute?
		if (baseDNName.startsWith(name))
			return name;

		return ((Name) baseDNName.clone()).addAll(name);
	}
}

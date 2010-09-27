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
package org.apache.directory.server.sar;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanRegistration;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.server.configuration.MutableServerStartupConfiguration;
import org.apache.directory.server.core.configuration.Configuration;
import org.apache.directory.server.core.configuration.MutablePartitionConfiguration;
import org.apache.directory.server.core.configuration.ShutdownConfiguration;
import org.apache.directory.server.core.configuration.SyncConfiguration;
import org.apache.directory.server.jndi.ServerContextFactory;
import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * JBoss 3.x Mbean for embedded and remote directory server support
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory
 *         Project</a>
 * @version $Rev: 379313 $, $Date: 2006-02-21 02:52:45 +0000 (Di, 21 Feb 2006) $
 */
public class DirectoryService extends ServiceMBeanSupport
		implements
			DirectoryServiceMBean,
			MBeanRegistration {
	// ~ Static fields/initializers
	// ---------------------------------------------

	private static final Logger LOG = LoggerFactory
			.getLogger(DirectoryService.class);

	/** Default LDAP Listen Port */
	public static final int DEFAULT_LDAP_PORT = 10389;

	/** Default LDAPS (SSL) Port */
	public static final int DEFAULT_LDAPS_PORT = 10636;

	// ~ Instance fields
	// --------------------------------------------------------

	private boolean embeddedServerEnabled = true;

	private String wkDir = ".";

	private String ldifDir = "./ldif";

	private int ldapPort = DEFAULT_LDAP_PORT;

	private int ldapsPort = DEFAULT_LDAPS_PORT;

	private String customRootPartitionName = "com";

	private String contextProviderURL = "uid=admin,ou=system";

	private String securityAuthentication = "simple";

	private String securityCredentials = System.getProperty(
			"ContextSecurityCredentials", "secret");

	private String securityPrincipal = "uid=admin,ou=system";

	private boolean anonymousAccess = false;

	private boolean ldapNetworkingSupport = false;

	private String contextFactory = ServerContextFactory.class.getName();

	private Element additionalEnv = null;

	private Element customSchema = null;

	private Element ldifFilters = null;

	private boolean accessControlEnabled = false;

	private boolean enableNtp = false;

	private boolean enableKerberos = false;

	private boolean enableChangePassword = false;

	private Timer syncTimer;

	// ~ Methods
	// ----------------------------------------------------------------

	protected void startService() throws Exception {
		// Build the properties from bean attributes
		final Hashtable env = createContextEnv();

		if (embeddedServerEnabled) {
			if (LOG.isInfoEnabled())
				LOG.info("Starting Embedded Directory Server...");

			// Create the baseline configuration
			final MutableServerStartupConfiguration cfg = new MutableServerStartupConfiguration();

			/*
			 * *************** Update the baseline configuration *****************
			 */
			// Access Control
			cfg.setAccessControlEnabled(this.accessControlEnabled);
			cfg.setAllowAnonymousAccess(this.anonymousAccess);

			// Wire protocols
			cfg.setEnableNetworking(this.ldapNetworkingSupport);
			cfg.setLdapPort(this.ldapPort);
			cfg.setLdapsPort(this.ldapsPort);
			// cfg.setEnableLdaps(true);

			cfg.setEnableNtp(enableNtp);
			cfg.setEnableKerberos(enableKerberos);
			cfg.setEnableChangePassword(enableChangePassword);

			// Work folder
			cfg.setWorkingDirectory(new File(this.wkDir));

			// LDIF import
			cfg.setLdifDirectory(new File(this.ldifDir));
			cfg.setLdifFilters(addCustomLdifFilters());

			// Addditional bootstrap schema
			cfg.setBootstrapSchemas(addCustomBootstrapSchema(cfg
					.getBootstrapSchemas()));

			// Single custom partition
			if (null != this.customRootPartitionName
					&& this.customRootPartitionName.length() > 0) {
				if (LOG.isDebugEnabled())
					LOG.debug("Adding custom root partition name: "
							+ this.customRootPartitionName);

				final Set pcfgs = addCustomPartition();
				cfg.setContextPartitionConfigurations(pcfgs);
			}

			// Put the configuration instruction to the environment variable.
			env.putAll(cfg.toJndiEnvironment());

			new InitialDirContext(env);

			// launch a flush timer
			syncTimer = new Timer(true);
			syncTimer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					flushEmbeddedServerData();
				}
			}, 0, 5000);
		} else if (LOG.isWarnEnabled())
			LOG.warn("No Embedded directory server requested.  All directory access will be via remote LDAP interface.");

		if (LOG.isDebugEnabled()) {
			LOG.debug("Directory Environment:");

			final Enumeration en = env.keys();

			while (en.hasMoreElements()) {
				final Object key = en.nextElement();
				LOG.debug("    " + key + ":" + env.get(key));
			}
		}
	}

	private List addCustomLdifFilters() {
		final List filters = new ArrayList();

		final Hashtable ht = getPropertiesFromElement(ldifFilters);
		final Enumeration en = ht.elements();
		Class clazz = null;

		while (en.hasMoreElements())
			try {
				clazz = Class.forName((String) en.nextElement());
				filters.add(clazz.newInstance());
			} catch (final Exception e) {
				if (LOG.isErrorEnabled())
					LOG.error(e.toString());
			}

		return filters;
	}

	private Set addCustomBootstrapSchema(Set schema) {
		final Hashtable ht = getPropertiesFromElement(customSchema);
		final Enumeration en = ht.elements();
		Class clazz = null;

		while (en.hasMoreElements())
			try {
				clazz = Class.forName((String) en.nextElement());
				schema.add(clazz.newInstance());
			} catch (final Exception e) {
				if (LOG.isErrorEnabled())
					LOG.error(e.toString());
			}

		return schema;
	}

	private void addAdditionalEnv(Hashtable env) {
		final Hashtable ht = getPropertiesFromElement(additionalEnv);
		final Enumeration en = ht.keys();
		String key = null;

		while (en.hasMoreElements()) {
			key = (String) en.nextElement();
			env.put(key, ht.get(key));
		}
	}

	private Hashtable createContextEnv() {
		final Hashtable env = new Properties();

		addAdditionalEnv(env);

		env.put(Context.PROVIDER_URL, this.contextProviderURL);
		env.put(Context.INITIAL_CONTEXT_FACTORY, this.contextFactory);

		env.put(Context.SECURITY_AUTHENTICATION, this.securityAuthentication);
		env.put(Context.SECURITY_PRINCIPAL, this.securityPrincipal);
		env.put(Context.SECURITY_CREDENTIALS, this.securityCredentials);

		if (this.isEmbeddedServerEnabled())
			// This is bug-or-wierdness workaround for in-VM access to the
			// DirContext of ApacheDS
			env.put(Configuration.JNDI_KEY, new SyncConfiguration());

		return env;
	}

	private Set addCustomPartition() throws NamingException {
		BasicAttributes attrs;
		Set indexedAttrs;
		BasicAttribute attr;
		final Set pcfgs = new HashSet();
		final MutablePartitionConfiguration pcfg = new MutablePartitionConfiguration();

		// construct partition name from DN
		final String nameParts[] = this.customRootPartitionName.split(",");
		final StringBuffer partitionName = new StringBuffer();
		for (int i = 0; i < nameParts.length; i++) {
			final int idx = nameParts[i].indexOf('=');
			if (i > 0)
				partitionName.append('_');
			partitionName.append(idx > 0
					? nameParts[i].substring(idx + 1)
					: nameParts[i]);
		}

		pcfg.setName(partitionName.toString());
		pcfg.setSuffix(this.customRootPartitionName);

		indexedAttrs = new HashSet();
		indexedAttrs.add("ou");
		indexedAttrs.add("dc");
		indexedAttrs.add("cn");
		indexedAttrs.add("macAddress");
		indexedAttrs.add("ipHostNumber");
		indexedAttrs.add("objectClass");
		pcfg.setIndexedAttributes(indexedAttrs);

		attrs = new BasicAttributes(true);

		attr = new BasicAttribute("objectClass");
		attr.add("top");
		attr.add("domain");
		attr.add("extensibleObject");
		attrs.put(attr);

		attr = new BasicAttribute("dc");
		attr.add(this.customRootPartitionName);
		attrs.put(attr);

		pcfg.setContextEntry(attrs);

		pcfgs.add(pcfg);

		return pcfgs;
	}

	protected void stopService() throws Exception {
		if (embeddedServerEnabled) {
			if (LOG.isInfoEnabled())
				LOG.info("Stopping Embedded Directory Server...");

			if (null != syncTimer) {
				syncTimer.cancel();
				syncTimer = null;
			}

			// Create a configuration instruction.
			final ShutdownConfiguration cfg = new ShutdownConfiguration();

			// Build the properties from bean attributes
			final Hashtable env = createContextEnv();

			// Put the configuration instruction to the environment variable.
			env.putAll(cfg.toJndiEnvironment());

			new InitialDirContext(env);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#getContextProviderURL
	 * ()
	 */
	public String getContextProviderURL() {
		return this.contextProviderURL;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * getContextSecurityAuthentication()
	 */
	public String getContextSecurityAuthentication() {
		return this.securityAuthentication;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * getContextSecurityCredentials()
	 */
	public String getContextSecurityCredentials() {
		return this.securityCredentials;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * getContextSecurityPrincipal()
	 */
	public String getContextSecurityPrincipal() {
		return this.securityPrincipal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * getEmbeddedCustomRootPartitionName()
	 */
	public String getEmbeddedCustomRootPartitionName() {
		return this.customRootPartitionName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#getEmbeddedLDAPPort()
	 */
	public int getEmbeddedLDAPPort() {
		return this.ldapPort;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#getEmbeddedLDAPSPort
	 * ()
	 */
	public int getEmbeddedLDAPSPort() {
		return this.ldapsPort;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#getEmbeddedLDIFdir()
	 */
	public String getEmbeddedLDIFdir() {
		return this.ldifDir;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#getEmbeddedWkdir()
	 */
	public String getEmbeddedWkdir() {
		return this.wkDir;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#isEmbeddedAnonymousAccess
	 * ()
	 */
	public boolean isEmbeddedAnonymousAccess() {
		return this.anonymousAccess;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * isEmbeddedLDAPNetworkingSupport()
	 */
	public boolean isEmbeddedLDAPNetworkingSupport() {
		return this.ldapNetworkingSupport;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#isEmbeddedServerEnabled
	 * ()
	 */
	public boolean isEmbeddedServerEnabled() {
		return this.embeddedServerEnabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#openDirContext()
	 */
	public DirContext openDirContext() throws NamingException {
		final Hashtable env = createContextEnv();

		return new InitialDirContext(env);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#setContextProviderURL
	 * (java.lang.String)
	 */
	public void setContextProviderURL(String providerURL) {
		this.contextProviderURL = providerURL;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * setContextSecurityAuthentication(java.lang.String)
	 */
	public void setContextSecurityAuthentication(String securityAuthentication) {
		this.securityAuthentication = securityAuthentication;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * setContextSecurityCredentials(java.lang.String)
	 */
	public void setContextSecurityCredentials(String securityCredentials) {
		this.securityCredentials = securityCredentials;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * setContextSecurityprincipal(java.lang.String)
	 */
	public void setContextSecurityPrincipal(String securityPrincipal) {
		this.securityPrincipal = securityPrincipal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * setEmbeddedAnonymousAccess(boolean)
	 */
	public void setEmbeddedAnonymousAccess(boolean anonymousAccess) {
		this.anonymousAccess = anonymousAccess;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * setEmbeddedCustomRootPartitionName(java.lang.String)
	 */
	public void setEmbeddedCustomRootPartitionName(String rootPartitianName) {
		this.customRootPartitionName = rootPartitianName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * setEmbeddedLDAPNetworkingSupport(boolean)
	 */
	public void setEmbeddedLDAPNetworkingSupport(boolean ldapNetworkingSupport) {
		this.ldapNetworkingSupport = ldapNetworkingSupport;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#setEmbeddedLDAPPort
	 * (int)
	 */
	public void setEmbeddedLDAPPort(int ldapPort) {
		this.ldapPort = ldapPort;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#setEmbeddedLDAPSPort
	 * (int)
	 */
	public void setEmbeddedLDAPSPort(int ldapsPort) {
		this.ldapsPort = ldapsPort;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#setEmbeddedLDIFdir
	 * (java.lang.String)
	 */
	public void setEmbeddedLDIFdir(String LDIFdir) {
		this.ldifDir = LDIFdir;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#setEmbeddedServerEnabled
	 * (boolean)
	 */
	public void setEmbeddedServerEnabled(boolean enabled) {
		this.embeddedServerEnabled = enabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#setEmbeddedWkdir(
	 * java.lang.String)
	 */
	public void setEmbeddedWkdir(String wkdir) {
		this.wkDir = wkdir;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#getContextFactory()
	 */
	public String getContextFactory() {
		return this.contextFactory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#setContextFactory
	 * (java.lang.String)
	 */
	public void setContextFactory(String factoryClass) {
		this.contextFactory = factoryClass;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * changedEmbeddedAdminPassword(java.lang.String, java.lang.String)
	 */
	public String changedEmbeddedAdminPassword(String oldPassword,
			String newPassword) {
		if (embeddedServerEnabled) {
			if (this.securityCredentials.equals(oldPassword)) {
				final ModificationItem[] mods = new ModificationItem[1];
				final Attribute password = new BasicAttribute("userpassword",
						newPassword);
				mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, password);

				try {
					final DirContext dc = openDirContext();

					dc.modifyAttributes("", mods);
					dc.close();
				} catch (final NamingException e) {
					final String msg = "Failed modifying directory password attribute: "
							+ e;

					if (LOG.isErrorEnabled())
						LOG.error(msg);

					return msg;
				}

				this.securityCredentials = newPassword;

				return "Password change successful.";
			} else
				return "Invalid oldPassword given.";
		} else {
			final String msg = "Unable to change password as embedded server is not enabled.";

			if (LOG.isWarnEnabled())
				LOG.warn(msg);

			return msg;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#flushEmbeddedServerData
	 * ()
	 */
	public boolean flushEmbeddedServerData() {
		if (embeddedServerEnabled)
			try {
				if (LOG.isDebugEnabled())
					LOG.debug("Syncing Embedded Directory Server...");

				// Create a configuration instruction.
				final SyncConfiguration cfg = new SyncConfiguration();

				// Build the properties from bean attributes
				final Hashtable env = createContextEnv();

				// Put the configuration instruction to the environment
				// variable.
				env.putAll(cfg.toJndiEnvironment());

				if (LOG.isDebugEnabled()) {
					LOG.info("Directory Properties:");

					final Enumeration en = env.keys();

					while (en.hasMoreElements()) {
						final Object key = en.nextElement();
						LOG.debug("    " + key + ":" + env.get(key));
					}
				}

				new InitialDirContext(env);

				return true;
			} catch (final NamingException e) {
				LOG.error("Can't flush server", e);
			}
		else
			LOG.warn("Unable to flush as embedded server is not enabled.");

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * getEmbeddedAdditionalEnvProperties()
	 */
	public Element getEmbeddedAdditionalEnvProperties() {
		return this.additionalEnv;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * getEmbeddedCustomBootstrapSchemas()
	 */
	public Element getEmbeddedCustomBootstrapSchema() {
		return this.customSchema;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * setEmbeddedAdditionalEnvProperties(java.util.Properties)
	 */
	public void setEmbeddedAdditionalEnvProperties(Element env) {
		this.additionalEnv = env;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * setEmbeddedCustomBootstrapSchemas(java.util.Properties)
	 */
	public void setEmbeddedCustomBootstrapSchema(Element cfg) {
		this.customSchema = cfg;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * isEmbeddedAccessControlEnabled()
	 */
	public boolean isEmbeddedAccessControlEnabled() {
		return this.accessControlEnabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * isEmbeddedEnableChangePassword()
	 */
	public boolean isEmbeddedEnableChangePassword() {
		return this.enableChangePassword;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#isEmbeddedEnableKerberos
	 * ()
	 */
	public boolean isEmbeddedEnableKerberos() {
		return this.enableKerberos;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#isEmbeddedEnableNtp()
	 */
	public boolean isEmbeddedEnableNtp() {
		return this.enableNtp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * setEmbeddedAccessControlEnabled(boolean)
	 */
	public void setEmbeddedAccessControlEnabled(boolean enabled) {
		this.accessControlEnabled = enabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
	 * setEmbeddedEnableChangePassword(boolean)
	 */
	public void setEmbeddedEnableChangePassword(boolean enabled) {
		this.enableChangePassword = enabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#setEmbeddedEnableKerberos
	 * (boolean)
	 */
	public void setEmbeddedEnableKerberos(boolean enabled) {
		this.enableKerberos = enabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#setEmbeddedEnableNtp
	 * (boolean)
	 */
	public void setEmbeddedEnableNtp(boolean enabled) {
		this.enableNtp = enabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#getEmbeddedLDIFFilters
	 * ()
	 */
	public Element getEmbeddedLDIFFilters() {
		return this.ldifFilters;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.jmx.DirectoryServiceMBean#setEmbeddedLDIFFilters
	 * (org.w3c.dom.Element)
	 */
	public void setEmbeddedLDIFFilters(Element fil) {
		this.ldifFilters = fil;
	}

	// Embedded lists inside the Mbean service definition are made available as
	// DOM elements
	// and are parsed into a java collection before use
	private Hashtable getPropertiesFromElement(Element element) {
		final Hashtable ht = new Hashtable();

		if (null != element) {
			if (LOG.isInfoEnabled())
				LOG.info("Adding custom configuration elements:");

			final NodeList nl = element.getChildNodes();
			Node el = null;

			for (int ii = 0; ii < nl.getLength(); ii++) {
				el = nl.item(ii);

				String val = null;
				String name = null;

				if (el.getNodeType() == Node.ELEMENT_NODE) {
					name = el.getAttributes().getNamedItem("name").getNodeValue();

					final NodeList vnl = el.getChildNodes();

					for (int jj = 0; jj < vnl.getLength(); jj++) {
						el = vnl.item(jj);

						if (el.getNodeType() == Node.TEXT_NODE) {
							val = el.getNodeValue();

							break;
						}
					}

					if (null != name && null != val) {
						if (LOG.isInfoEnabled())
							LOG.info("    " + name + ": " + val);

						ht.put(name, val);

						break;
					}
				}
			}
		}

		return ht;
	}
}

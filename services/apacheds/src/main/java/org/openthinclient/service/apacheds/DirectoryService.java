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
package org.openthinclient.service.apacheds;

import org.apache.directory.server.configuration.MutableServerStartupConfiguration;
import org.apache.directory.server.core.configuration.Configuration;
import org.apache.directory.server.core.configuration.MutablePartitionConfiguration;
import org.apache.directory.server.core.configuration.ShutdownConfiguration;
import org.apache.directory.server.core.configuration.SyncConfiguration;
import org.apache.directory.server.core.schema.bootstrap.BootstrapSchema;
import org.apache.directory.server.core.schema.bootstrap.NisSchema;
import org.apache.directory.server.jndi.ServerContextFactory;
import org.openthinclient.service.common.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;

public class DirectoryService
        implements Service<DirectoryServiceConfiguration> {

  private static final Logger LOG = LoggerFactory
          .getLogger(DirectoryService.class);
  private DirectoryServiceConfiguration configuration;
  private Timer syncTimer;

  @Override
  public DirectoryServiceConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public void setConfiguration(DirectoryServiceConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public Class<DirectoryServiceConfiguration> getConfigurationClass() {
    return DirectoryServiceConfiguration.class;
  }

  // ~ Methods
  // ----------------------------------------------------------------

  @Override
  public void startService() throws Exception {
    // Build the properties from bean attributes
    final Hashtable env = createContextEnv();

    if (configuration.isEmbeddedServerEnabled()) {
      if (LOG.isInfoEnabled())
        LOG.info("Starting Embedded Directory Server...");

      // Create the baseline configuration
      final MutableServerStartupConfiguration cfg = new MutableServerStartupConfiguration();

			/*
       * *************** Update the baseline configuration *****************
			 */
      // Access Control
      cfg.setAccessControlEnabled(configuration.isAccessControlEnabled());
      cfg.setAllowAnonymousAccess(configuration.isEmbeddedAnonymousAccess());

      // Wire protocols
      cfg.setEnableNetworking(true);
      cfg.setLdapPort(configuration.getEmbeddedLdapPort());
      cfg.setLdapsPort(configuration.getEmbeddedLdapsPort());
      // cfg.setEnableLdaps(true);

      cfg.setEnableNtp(configuration.isEnableNtp());
      cfg.setEnableKerberos(configuration.isEnableKerberos());
      cfg.setEnableChangePassword(configuration.isEnableChangePassword());

      // Work folder
      cfg.setWorkingDirectory(configuration.getEmbeddedWkDir());

      // LDIF import
      cfg.setLdifDirectory(configuration.getEmbeddedLDIFDir());
      cfg.setLdifFilters(addCustomLdifFilters());

      // Addditional bootstrap schema
      cfg.setBootstrapSchemas(addCustomBootstrapSchema(cfg
              .getBootstrapSchemas()));

      // Single custom partition
      if (null != configuration.getEmbeddedCustomRootPartitionName()
              && configuration.getEmbeddedCustomRootPartitionName().length() > 0) {
        if (LOG.isDebugEnabled())
          LOG.debug("Adding custom root partition name: "
                  + configuration.getEmbeddedCustomRootPartitionName());

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

    final Hashtable ht = getPropertiesFromElement(configuration.getLdifFilters());
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

  private Set<BootstrapSchema> addCustomBootstrapSchema(Set<BootstrapSchema> schema) {

    schema.add(new NisSchema());

    return schema;
  }

  private void addAdditionalEnv(Hashtable env) {
    final Hashtable ht = getPropertiesFromElement(configuration.getAdditionalEnv());
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

    env.put(Context.PROVIDER_URL, configuration.getContextProviderURL());
    env.put(Context.INITIAL_CONTEXT_FACTORY, ServerContextFactory.class.getName());

    env.put(Context.SECURITY_AUTHENTICATION, configuration.getContextSecurityAuthentication());
    env.put(Context.SECURITY_PRINCIPAL, configuration.getContextSecurityPrincipal());
    env.put(Context.SECURITY_CREDENTIALS, configuration.getContextSecurityCredentials());

    if (configuration.isEmbeddedServerEnabled())
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
    final String nameParts[] = configuration.getEmbeddedCustomRootPartitionName().split(",");
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
    pcfg.setSuffix(configuration.getEmbeddedCustomRootPartitionName());

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
    attr.add(configuration.getEmbeddedCustomRootPartitionName());
    attrs.put(attr);

    pcfg.setContextEntry(attrs);

    pcfgs.add(pcfg);

    return pcfgs;
  }

  @Override
  public void stopService() throws Exception {
    if (configuration.isEmbeddedServerEnabled()) {
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

      // try to bind the port used by the apacheds.
      // This will ensure that the apacheds is really down when stopService returns. This will ensure
      // that the server will be down once the first start wizard finishes and restarts the server.
      // If not, the restart process will be initiated and the shutdown of the apacheds might not
      // yet be completed. In that case, starting the DS service again, will fail due to the port
      // still being in use.

      for(int retries = 0; retries <= 10; retries++) {
        LOG.info("Verifying directory service shutdown complete...");
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        try {
          final ServerSocket socket = new ServerSocket(configuration.getEmbeddedLdapPort());
          socket.close();
          return;
        } catch (Exception e) {
          LOG.info("Directory service still alive. Waiting for shutdown...");
          LOG.debug("Exception trying to acquire the server socket", e);
        }
      }

    }
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
   * @see org.apache.directory.server.jmx.DirectoryServiceMBean#
   * changedEmbeddedAdminPassword(java.lang.String, java.lang.String)
   */
  public String changedEmbeddedAdminPassword(String oldPassword,
                                             String newPassword) {
    if (configuration.isEmbeddedServerEnabled()) {
      if (configuration.getContextSecurityCredentials().equals(oldPassword)) {
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

        // FIXME: what about persisting those changes?
        configuration.setContextSecurityCredentials(newPassword);

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
    if (configuration.isEmbeddedServerEnabled())
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

  // Embedded lists inside the Mbean service definition are made available as
  // DOM elements
  // and are parsed into a java collection before use
  private Hashtable getPropertiesFromElement(Element element) {
    final Hashtable ht = new Hashtable();

    // remove the wrapping element around the actual content that we're interested in
    if (element != null && element.getFirstChild() instanceof Element)
      element = (Element) element.getFirstChild();

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

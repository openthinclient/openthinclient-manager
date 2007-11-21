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
package org.openthinclient.common.model;

import java.io.Serializable;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.HTTPSchemaProvider;
import org.openthinclient.common.model.schema.provider.LocalSchemaProvider;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

/**
 * @author levigo
 */
public class Realm extends Profile implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(Realm.class);

	private LDAPConnectionDescriptor lcd;

	private transient UserGroup administrators;
	private transient User readOnlyPrincipal;

	private transient LDAPDirectory directory;
	private transient boolean needRefresh;
	private transient SchemaProvider schemaProvider;

	private String schemaProviderName;

	public Realm() {

	}

	/**
	 * Create Realm from connection descriptor and immediately refresh it.
	 * 
	 * @param lcd
	 * @throws DirectoryException
	 */
	public Realm(LDAPConnectionDescriptor lcd) throws DirectoryException {
		this.lcd = lcd;

		setDn(LDAPDirectory.REALM_RDN);

		refresh();
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		final String sb = new StringBuffer("[Realm url=").append(
				lcd != null ? lcd.getLDAPUrl() : "?").append(", description=").append(
				getDescription()).append("}]").toString();

		if (logger.isDebugEnabled())
			logger.debug("Realm: " + sb);

		return new StringBuffer("[Realm url=").append(
				lcd != null ? lcd.getLDAPUrl() : "?").append(", description=").append(
				getDescription()).append("}]").toString();
	}

	/**
	 * @param lcd
	 * 
	 */
	public void setConnectionDescriptor(LDAPConnectionDescriptor lcd) {
		this.lcd = lcd;
	}

	public LDAPDirectory getDirectory() throws DirectoryException {
		if (null == directory)
			directory = LDAPDirectory.openRealm(this);
		return directory;
	}

	public void closeDirectory() {
		directory = null;
	}

	public void refresh() throws DirectoryException {
		directory = null;
		getDirectory().refresh(this);
		needRefresh = false;
	}

	public void ensureInitialized() throws DirectoryException {
		refresh();
		getDirectory();
	}

	/**
	 * @return
	 */
	public LDAPConnectionDescriptor getConnectionDescriptor() {
		return lcd;
	}

	public void setNeedsRefresh() {
		this.needRefresh = true;
	}

	@Override
	public boolean containsValue(String key) {

		checkRefresh();
		return super.containsValue(key);
	}

	/**
	 * 
	 */
	private void checkRefresh() {
		if (needRefresh)
			try {
				refresh();
			} catch (final DirectoryException e) {
				// convert to runtime exception
				throw new RuntimeException("Unexpected exception during realm refresh",
						e);
			}
	}

	@Override
	public String getDefiningProfile(String path, boolean excludeThis) {
		checkRefresh();
		return super.getDefiningProfile(path, excludeThis);
	}

	@Override
	public String getOverriddenValue(String key) {
		checkRefresh();
		return super.getOverriddenValue(key);
	}

	@Override
	public Schema getSchema() throws SchemaLoadingException {
		checkRefresh();
		return super.getSchema();
	}

	@Override
	public String getValue(String key) {
		checkRefresh();
		return super.getValue(key);
	}

	@Override
	public boolean inherits(String key) {
		checkRefresh();
		return super.inherits(key);
	}

	@Override
	public void removeValue(String key) {
		checkRefresh();
		super.removeValue(key);
	}

	@Override
	public void setSchema(Schema schema) {
		checkRefresh();
		super.setSchema(schema);
	}

	@Override
	public void setValue(String path, String value) {
		checkRefresh();
		super.setValue(path, value);
	}

	public UserGroup getAdministrators() {
		if (null == administrators) {
			administrators = new UserGroup();
			administrators.setName("administrators");
		}
		return administrators;
	}

	public void setAdministrators(UserGroup administrators) {
		this.administrators = administrators;
	}

	/**
	 * @return
	 */
	public SchemaProvider getSchemaProvider() {
		if (null == schemaProvider)
			schemaProvider = createSchemaProvider();
		return schemaProvider;
	}

	public void setSchemaProviderName(String providerName) {
		this.schemaProviderName = providerName;
	}

	public String getSchemaProviderName() {
		return this.schemaProviderName;
	}

	public void setSchemaProvider(SchemaProvider schemaProvider) {
		this.schemaProvider = schemaProvider;
	}

	/**
	 * @return
	 */

	private SchemaProvider createSchemaProvider() {

		final String newServerName = this
				.getValue("Serversettings.SchemaProviderName");

		if (newServerName != null) {
			try {
				final HTTPSchemaProvider provider = new HTTPSchemaProvider(
						newServerName);

				if (provider.checkAccess()) {
					if (logger.isDebugEnabled())
						logger.debug("Using " + newServerName);
					return provider;
				} else if (logger.isDebugEnabled())
					logger.debug("Can't use " + newServerName);
			} catch (final MalformedURLException e) {
				logger.error("Invalid server URL for " + newServerName, e);
			}
			if (logger.isDebugEnabled())
				logger.debug("No usable servers found - falling back to local schemas");
		} else
			try {
				throw new SchemaLoadingException("Schema wasn't found");
			} catch (final SchemaLoadingException e) {
				e.printStackTrace();
			}
		// else {
		//  	
		// String servers = getValue("Servers.FileServiceServers");
		// // String servers = getValue("Serversettings.SchemaProviderName");
		// List<String> serverNames = new LinkedList<String>();
		// if (null != servers)
		// for (String serverName : servers.split("\\s*;\\s*"))
		// if (serverName.length() > 0)
		// serverNames.add(serverName);
		// serverNames.add(lcd.getHostname());
		//
		// logger.info("Trying the following FileService servers: " + serverNames);
		//
		// for (String serverName : serverNames) {
		// try {
		// HTTPSchemaProvider provider = new HTTPSchemaProvider(serverName);
		// if (provider.checkAccess()) {
		// logger.info("Using " + serverName);
		// return provider;
		// } else
		// logger.info("Can't use " + serverName);
		// } catch (MalformedURLException e) {
		// logger.error("Invalid server URL for " + serverName, e);
		// }
		// }
		//
		// logger.info("No usable servers found - falling back to local schemas");
		// }
		return LocalSchemaProvider.getInstance();
	}

	/*
	 * @see org.openthinclient.common.model.DirectoryObject#getName()
	 */
	@Override
	public String getName() {
		if (getDescription() == null || getDescription().length() == 0)
			return getDn();
		else
			return getDn() + " (" + getDescription() + ")"; //$NON-NLS-1$ //$NON-NLS-2$;
	}

	public User getReadOnlyPrincipal() {
		if (null == readOnlyPrincipal) {
			readOnlyPrincipal = new User();
			readOnlyPrincipal.setName("roPrincipal");
			readOnlyPrincipal.setSn("Read Only User");
		}
		return readOnlyPrincipal;
	}

	public void setReadOnlyPrincipal(User readOnlyPrincipal) {
		this.readOnlyPrincipal = readOnlyPrincipal;
	}

	/**
	 * This is a HACK. It will go away, so don't use it, please.
	 */
	public void fakePropertyChange() {
		firePropertyChange("user", "old", "new");
	}

	public void removeSchemaProvider() {
		schemaProvider = null;
	}

	@Override
	protected String getSchemaName() {
		return "realm";
	}
}

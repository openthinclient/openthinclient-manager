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
import java.util.LinkedList;
import java.util.List;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.HTTPSchemaProvider;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;

import com.sun.jndi.ldap.LdapURL;

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
	private boolean isInitialized;

	public Realm() {

	}

	/**
	 * Create Realm from connection descriptor.
	 * 
	 * @param lcd
	 * @throws DirectoryException
	 */
	public Realm(LDAPConnectionDescriptor lcd) throws DirectoryException {
		this.lcd = lcd;

		setDn(LDAPDirectory.REALM_RDN);
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
		needRefresh = false;
		directory = null;
		getDirectory().refresh(this);
	}

	public void ensureInitialized() throws DirectoryException {
		if (false == isInitialized) {
			getDirectory().refresh(this);
			refresh();
		}
		isInitialized = true;
	}

	/**
	 * @return
	 */
	public LDAPConnectionDescriptor getConnectionDescriptor() {
		return lcd;
	}

	/**
	 * @author goldml
	 * 
	 * The method will create a new connection to a secondary server.
	 */
	public LDAPConnectionDescriptor createSecondaryConnectionDescriptor()
			throws DirectoryException {
		final LDAPConnectionDescriptor secLcd = new LDAPConnectionDescriptor();

		final String urlString = getValue("Directory.Secondary.LDAPURLs");

		try {
			final LdapURL ldapUrl = new LdapURL(urlString);

			secLcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
			secLcd.setHostname(ldapUrl.getHost());
			secLcd.setPortNumber((short) ldapUrl.getPort());
			secLcd.setBaseDN(ldapUrl.getDN());
			final String principal = getValue("Directory.Secondary.ReadOnly.Principal");
			final String secret = getValue("Directory.Secondary.ReadOnly.Secret");

			if (null != principal) {
				secLcd
						.setCallbackHandler(new UsernamePasswordHandler(principal, secret));
				secLcd
						.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
			} else
				secLcd
						.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.NONE);

			// for read only
			final String asd = getValue("UserGroupSettings.Type");
			if (asd.equals("NewUsersGroups"))
				secLcd.setReadOnly(false);
			else
				secLcd.setReadOnly(true);

		} catch (final NamingException e) {
			e.printStackTrace();
		}
		return secLcd;
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
	public Schema getSchema(Realm realm) throws SchemaLoadingException {
		checkRefresh();
		return super.getSchema(realm);
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
	 * @throws SchemaLoadingException
	 */
	public SchemaProvider getSchemaProvider() throws SchemaLoadingException {
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
	 * @throws SchemaLoadingException
	 */

	private SchemaProvider createSchemaProvider() throws SchemaLoadingException {
		final List<String> schemaProviderHosts = new LinkedList<String>();
		String schemaProviderHost = this
				.getValue("Serversettings.SchemaProviderName");

		if (null == schemaProviderHost)
			schemaProviderHost = lcd.getHostname();

		if (null != schemaProviderHost)
			schemaProviderHosts.add(schemaProviderHost);

		// Always have localhost as fallback to reach a misconfigured
		// schemaProviderHost
		schemaProviderHosts.add("localhost");

		for (final String host : schemaProviderHosts)
			if (host != null) {
				try {
					final HTTPSchemaProvider provider = new HTTPSchemaProvider(host);

					if (provider.checkAccess()) {
						if (logger.isDebugEnabled())
							logger.debug("Using " + host);
						return provider;
					} else if (logger.isDebugEnabled())
						logger.debug("Can't use " + host);
				} catch (final MalformedURLException e) {
					logger.error("Invalid server URL for " + host, e);
				}
				if (logger.isDebugEnabled() && host == "localhost")
					logger
							.warn("No usable servers found - falling back to local schemas");
			}
		throw new SchemaLoadingException(
				"Schema wasn't found: schema provider could not be determined");
	}

	/*
	 * @see org.openthinclient.common.model.DirectoryObject#getName()
	 */
	@Override
	public String getName() {
		return "RealmConfiguration"; // Realms have a fixed RDN (and thus name)
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

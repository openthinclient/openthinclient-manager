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

import java.util.TreeMap;

import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;

/**
 * The precedence for profile inheritance is:
 * <nl>
 * <li>My own (local) value
 * <li>A value of one of the inherited profiles
 * <li>The default of the local schema
 * <li>The inherited value of one of the inherited profiles
 * </nl>
 * 
 * @author levigo
 */
public abstract class Profile extends DirectoryObject {
	private static final long serialVersionUID = 1L;

	private transient Schema schema;

	private transient Properties properties;

	/*
	 * @see org.openthinclient.Profile#getValue(javax.swing.tree.String, boolean)
	 */
	public String getValue(String key) {
		final String myValue = getValueLocal(key);
		if (null != myValue)
			return myValue;

		return getInheritedValue(key);
	}

	/*
	 * @see org.openthinclient.Profile#getValue(javax.swing.tree.String, boolean)
	 */
	public String getValueLocal(String key) {
		return getProperties().getMap().get(key);
	}

	/**
	 * @param key
	 * @return
	 */
	private String getInheritedValue(String key) {
		String value = null;

		for (final Profile inherited : getInheritedProfiles())
			if (null != inherited) {
				value = inherited.getValueLocal(key);
				if (null != value)
					return value;
			}

		if (null != schema)
			value = schema.getValue(key);
		if (null != value)
			return value;

		for (final Profile inherited : getInheritedProfiles())
			if (null != inherited && !inherited.getName().equals(this.getName())) {
				value = inherited.getInheritedValue(key);
				if (null != value)
					return value;
			}

		return null;
	}

	protected Profile[] getInheritedProfiles() {
		final Profile none[] = {};
		return none;
	}

	/*
	 * @see org.openthinclient.common.model.Profile#setValue(java.lang.String,
	 *      java.lang.String)
	 */
	public void setValue(String path, String value) {
		getProperties().getMap().put(path, value);
	}

	public void removeValue(String key) {
		getProperties().getMap().remove(key);
	}

	/*
	 * @see org.openthinclient.Profile#containsPath(javax.swing.tree.String)
	 */
	public boolean containsValue(String key) {
		return null != properties && getProperties().getMap().containsKey(key);
	}

	public boolean inherits(String key) {
		final Profile[] inheritedProfiles = getInheritedProfiles();
		return inheritedProfiles != null;
	}

	/*
	 * @see org.openthinclient.Profile#getOverriddenValue(javax.swing.tree.String)
	 */
	public String getOverriddenValue(String key) {
		return getInheritedValue(key);
	}

	/*
	 * @see org.openthinclient.Profile#getDefiningProfile(javax.swing.tree.String)
	 */
	public String getDefiningProfile(String path, boolean excludeThis) {
		if (!excludeThis && containsValue(path))
			return getName();

		for (final Profile inherited : getInheritedProfiles())
			if (null != inherited && inherited.containsValue(path))
				return inherited.getName();

		if (schema.contains(path))
			return "Schema '" + schema.getName() + "'";

		for (final Profile inherited : getInheritedProfiles())
			if (null != inherited) {
				final String definingProfile = inherited.getDefiningProfile(path,
						excludeThis);
				if (null != definingProfile)
					return definingProfile;
			}

		return null;
	}

	/**
	 * Sets the schema for this Profile. The Propertis for this schema type are
	 * generated too.
	 * 
	 * @param schema the Schema to set
	 */
	public void setSchema(Schema schema) {
		this.schema = schema;
	}

	/**
	 * Get Schema for this Profile. If Schema doesn't exist but
	 * Properties.description is already set Schema is loaded here.
	 * 
	 * @param realm TODO
	 * 
	 * @return Schema of this Profile
	 * @throws SchemaLoadingException
	 */
	public Schema getSchema(Realm realm) throws SchemaLoadingException {
		loadSchema(realm);
		return schema;
	}

	/**
	 * @param realm
	 * @throws SchemaLoadingException
	 */
	public void initSchemas(Realm realm) throws SchemaLoadingException {
		loadSchema(realm);

		for (final Profile inherited : getInheritedProfiles())
			if (null != inherited)
				inherited.initSchemas(realm);
	}

	/**
	 * @param realm TODO
	 * @throws SchemaLoadingException
	 */
	private void loadSchema(Realm realm) throws SchemaLoadingException {
		final String schemaName = getSchemaName();

		if(realm != null) {
			schema = realm.getSchemaProvider().getSchema(this.getClass(), schemaName);
		}

		if (null == schema)
			throw new SchemaLoadingException("Schema wasn't found for " + getClass()
					+ (null != schemaName ? ", schemaName=" + schemaName : ""));
	}

	/**
	 * @return
	 */
	protected String getSchemaName() {
		return getProperties().getDescription();
	}

	/**
	 * Used for unmarshalling this profile from LDAP.
	 * 
	 * @param props
	 * @deprecated To be used by the LDAP mapping only
	 */
	@Deprecated
	public final void setProperties(Properties props) {
		this.properties = props;
	}

	/**
	 * Used for marshalling this profile to LDAP.
	 * 
	 * @return
	 * @deprecated To be used by the LDAP mapping only
	 */
	@Deprecated
	public final Properties getProperties() {
		if (null == properties)
			properties = new Properties("profile", "unknown",
					new TreeMap<String, String>());

		return properties;
	}
}

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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class is used mainly for marshalling/unmarshalling to/from ldap. It
 * carries a set of properties consisting of name/value pairs.
 * 
 * @author levigo
 */
public class Properties extends DirectoryObject {
	private static final long serialVersionUID = 1L;

	/**
	 * The properties kept in a key-value map for fast access. This map is not
	 * actually persisted. Persistence is realized by {@link #propertySet}.
	 */
	private SortedMap<String, String> propertyMap;

	/**
	 * This is the "persisten" set of properties. It is accessed by the LDAP
	 * mapping only (via {@link #getProperties()}/{@link #setProperties(Set)})
	 * and mirrored into the {@link #propertyMap}.
	 */
	private Set<Property> propertySet;

	/**
	 * @deprecated for LDAP mapping only!
	 */
	@Deprecated
	public Properties() {
	}

	/**
	 * @param string
	 * @param schemaName
	 * @param properties
	 */
	public Properties(String string, String schemaName,
			SortedMap<String, String> properties) {
		setName(string);
		setDescription(schemaName);
		setMap(properties);
	}

	/**
	 * Used by LDAP mapping.
	 * 
	 * @return
	 * @deprecated To be used by the LDAP mapping only
	 */
	@Deprecated
	public Set<Property> getProperties() {
		// if the properties haven't been accessed via the map, we
		// can safely return the initial set...
		if(null != propertySet)
			return propertySet;
		
		// ...otherwise we copy it back from the map. 
		Set<Property> props = new HashSet<Property>();
		if (null != propertyMap)
			for (Map.Entry<String, String> e : propertyMap.entrySet())
				props.add(new Property(this, e.getKey(), e.getValue()));
		
		return props;
	}

	/**
	 * Used my LDAP mapping.
	 * 
	 * @param props
	 * @deprecated To be used by the LDAP mapping only
	 */
	@Deprecated
	public void setProperties(Set<Property> props) {
		this.propertySet = props;

		// map will be initialized on demand later
		this.propertyMap = null;
	}

	/**
	 * @return
	 */
	public SortedMap<String, String> getMap() {
		if (null == propertyMap) {
			// trigger lazy proxy loading.
			propertySet.size();

			propertyMap = new TreeMap<String, String>();

			if (null != propertySet)
				for (Property p : propertySet)
					propertyMap.put(p.getName(), p.getValue());

			propertySet = null;
		}
		return propertyMap;
	}

	/**
	 * @param values
	 */
	private void setMap(SortedMap<String, String> values) {
		this.propertyMap = values;
	}

	public String getNisMapName() {
		return (this.getName());
	}

	public void setNisMapName(String name) {
		// ignored!
	}
}

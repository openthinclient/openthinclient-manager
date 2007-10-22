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
 *******************************************************************************/
package org.openthinclient.common.model;

/**
 * This class is used mainly for marshalling/unmarshalling to/from ldap. It
 * holds a name/value pair.
 * 
 * @author levigo
 */
public class Property extends DirectoryObject {
	private static final long serialVersionUID = 1L;

	private Properties parent;

	private String value;

	/**
	 * @deprecated To be used by the LDAP mapping only
	 */
	@Deprecated
	public Property() {
	}

	/**
	 * @param key
	 * @param value
	 */
	Property(Properties parent, String key, String value) {
		this.parent = parent;
		setName(key);
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getNisMapName() {
		return null != parent ? parent.getName() : "profile";
	}

	public void setNisMapName(String name) {
		// ignored!
	}

	public Properties getParent() {
		return parent;
	}

	public void setParent(Properties parent) {
		this.parent = parent;
	}
}

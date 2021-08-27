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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;

/**
 * A Client object with only minor attributes
 * @author levigo
 */
public class ClientMetaData extends Profile {

	private static final long serialVersionUID = 1L;

	private transient Schema schema;

	String ipHostNumber;
	String macAddress;
	Location location;

	public String getIpHostNumber() {
		if (null == ipHostNumber)
			return "0.0.0.0";
		return ipHostNumber;
	}

	public void setIpHostNumber(String ipHostNumber) {
		final String oldIpAddress = this.ipHostNumber;
		this.ipHostNumber = ipHostNumber;
		firePropertyChange("ipHostNumber", oldIpAddress, ipHostNumber);
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
		firePropertyChange("location", null, location);
	}

	@Override
	protected Class<? extends Profile> getSchemaClass() {
		return Client.class;
	}

	@Override
	public String toString() {
		return String.format(
			"[ClientMetaData %s, MAC=%s, IP=%s]",
			getName(), macAddress, ipHostNumber
		);
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		final String oldMacAddress = this.macAddress;
		this.macAddress = macAddress.toLowerCase();
		firePropertyChange("macAddress", oldMacAddress, macAddress);
	}

}

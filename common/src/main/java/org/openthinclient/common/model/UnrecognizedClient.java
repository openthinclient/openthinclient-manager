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

/**
 * @author levigo
 */
public class UnrecognizedClient extends DirectoryObject {
	private static final long serialVersionUID = 1L;

	private String ipHostNumber;
	private String macAddress;

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		final String oldMacAddress = this.macAddress;
		this.macAddress = macAddress.toLowerCase();
		firePropertyChange("macAddress", oldMacAddress, macAddress);
	}

	public void setIpHostNumber(String ipHostNumber) {
		final String oldIpAddress = this.ipHostNumber;
		this.ipHostNumber = ipHostNumber;
		firePropertyChange("ipHostNumber", oldIpAddress, ipHostNumber);
	}

	public String getIpHostNumber() {
		if (null == ipHostNumber)
			return "0.0.0.0";
		return ipHostNumber;
	}
}

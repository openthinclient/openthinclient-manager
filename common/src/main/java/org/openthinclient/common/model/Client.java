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

/**
 * @author levigo
 */
public class Client extends ClientMetaData implements AssociatedObjectsProvider {
	private static final long serialVersionUID = 1L;

	private Set<ApplicationGroup> applicationGroups;
	private Set<Application> applications;
	private Set<Printer> printers;
	private Set<Device> devices;
	private Set<ClientGroup> clientGroups;
	private HardwareType hardwareType;


	public Set<ApplicationGroup> getApplicationGroups() {
		return applicationGroups;
	}

	public Set<Application> getApplications() {
		return applications;
	}

	public void setApplicationGroups(Set<ApplicationGroup> applicationGroups) {
		this.applicationGroups = applicationGroups;
	}

	public void setApplications(Set<Application> applications) {
		this.applications = applications;
	}

	public void setHardwareType(HardwareType hardwareType) {
		this.hardwareType = hardwareType;
		firePropertyChange("hardwareType", null, hardwareType);
	}

	public HardwareType getHardwareType() {
		return hardwareType;
	}

	public Set<ClientGroup> getClientGroups() {
		return clientGroups;
	}

	public void setClientGroups(Set<ClientGroup> clientGroups) {
		this.clientGroups = clientGroups;

	}

	/**
	 * This method is used to beat the actually single-valued hardware type into
	 * the set semantics required by the ldap mapping.
	 *
	 * @deprecated for LDAP mapping only
	 */
	@Deprecated
	public void setHwTypes(Set<HardwareType> hardwareType) {
		this.hardwareType = hardwareType.size() > 0 ? hardwareType.iterator()
				.next() : null;
	}

	/**
	 * This method is used to beat the actually single-valued hardware type into
	 * the set semantics required by the ldap mapping.
	 *
	 * @deprecated for LDAP mapping only
	 */
	@Deprecated
	public Set<HardwareType> getHwTypes() {
		final Set<HardwareType> set = new HashSet<HardwareType>();
		if (null != hardwareType)
			set.add(hardwareType);
		return set;
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format(
			"[Client %s, MAC=%s, IP=%s, hwtype=%s]",
			getName(), macAddress, ipHostNumber, hardwareType
		);
	}

	public Set<Printer> getPrinters() {
		return printers;
	}

	public void setPrinters(Set<Printer> printers) {
		this.printers = printers;
		firePropertyChange("printers", null, printers);
	}

	public Set<Device> getDevices() {
		return devices;
	}

	public void setDevices(Set<Device> devices) {
		this.devices = devices;
		firePropertyChange("devices", null, devices);
	}

	/*
	 * @see org.openthinclient.common.model.DirectoryObject#getAssociatedObjects()
	 */
	public Map<Class, Set<? extends DirectoryObject>> getAssociatedObjects() {
		final Map<Class, Set<? extends DirectoryObject>> assocObjects = new HashMap<Class, Set<? extends DirectoryObject>>();
		assocObjects.put(Application.class, applications);
		assocObjects.put(ApplicationGroup.class, applicationGroups);
		assocObjects.put(ClientGroup.class, clientGroups);
		assocObjects.put(Printer.class, printers);
		assocObjects.put(Device.class, devices);
		return assocObjects;
	}

	/*
	 * @see org.openthinclient.common.model.DirectoryObject#setAssociatedObjects(java.lang.Class,
	 *      java.util.Set)
	 */
	public void setAssociatedObjects(Class subgroupClass, Set<? extends DirectoryObject> subgroups) {
		if (subgroupClass.equals(Application.class))
			setApplications((Set<Application>) subgroups);
		if (subgroupClass.equals(ApplicationGroup.class))
			setApplicationGroups((Set<ApplicationGroup>) subgroups);
		if (subgroupClass.equals(ClientGroup.class))
			setClientGroups((Set<ClientGroup>) subgroups);
		if (subgroupClass.equals(Printer.class))
			setPrinters((Set<Printer>) subgroups);
		if (subgroupClass.equals(Device.class))
			setDevices((Set<Device>) subgroups);
	}

	/*
	 * @see org.openthinclient.common.model.Profile#getInheritedProfiles()
	 */
	@Override
	protected Profile[] getInheritedProfiles() {
		return new Profile[]{hardwareType, location, getRealm()};
	}
}

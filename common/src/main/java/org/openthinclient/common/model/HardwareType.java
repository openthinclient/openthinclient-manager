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
import java.util.Map;
import java.util.Set;

/**
 * @author levigo
 */
public class HardwareType extends Profile
		implements
			Group<Client>,
			AssociatedObjectsProvider {
	private static final long serialVersionUID = 1L;

	// private static final Class[] MEMBER_CLASSES = new Class[]{Client.class,
	// HardwareType.class};
	private static final Class[] MEMBER_CLASSES = new Class[]{Client.class};

	private Set<Device> devices;
	private Set<HardwareType> hardwareTypes;

	private Set<Client> members;

	public Set<Device> getDevices() {
		return devices;
	}

	public void setDevices(Set<Device> devices) {
		this.devices = devices;
		firePropertyChange("devices", null, devices);
	}

	public Set<HardwareType> getHardwareTypes() {
		return hardwareTypes;
	}

	public void setHardwareTypes(Set<HardwareType> hardwareTypes) {
		this.hardwareTypes = hardwareTypes;
		firePropertyChange("hardwareTypes", null, hardwareTypes);
	}

	/*
	 * @see org.openthinclient.common.model.AssociatedObjectsProvider#getAssociatedObjects()
	 */
	public Map<Class, Set<? extends DirectoryObject>> getAssociatedObjects() {
		final Map<Class, Set<? extends DirectoryObject>> subgroups = new HashMap<Class, Set<? extends DirectoryObject>>();
		subgroups.put(Device.class, devices);
		subgroups.put(HardwareType.class, hardwareTypes);
		return subgroups;
	}

	/*
	 * @see org.openthinclient.common.model.AssociatedObjectsProvider#setAssociatedObjects(java.lang.Class,
	 *      java.util.Set)
	 */
	public void setAssociatedObjects(Class subgroupClass,
			Set<? extends DirectoryObject> subgroups) {
		if (subgroupClass.equals(Device.class))
			setDevices((Set<Device>) subgroups);
		if (subgroupClass.equals(HardwareType.class))
			setHardwareTypes((Set<HardwareType>) subgroups);

	}

	/*
	 * @see org.openthinclient.common.model.Group#getMemberClasses()
	 */
	public Class[] getMemberClasses() {
		return MEMBER_CLASSES;
	}

	/*
	 * @see org.openthinclient.common.model.Group#getMembers()
	 */
	public Set<Client> getMembers() {
		return members;
	}

	/*
	 * @see org.openthinclient.common.model.Group#setMembers(java.util.Set)
	 */
	public void setMembers(Set<Client> members) {
		this.members = members;
	}

	/*
	 * @see org.openthinclient.common.model.Profile#getInheritedProfiles()
	 */
	@Override
	protected Profile[] getInheritedProfiles() {
		return null != hardwareTypes ? hardwareTypes
				.toArray(new Profile[hardwareTypes.size()]) : super
				.getInheritedProfiles();
	}
}

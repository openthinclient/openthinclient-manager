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
 * @author andreas
 */
public class ClientGroup extends DirectoryObject implements
			Group<Client>,
			AssociatedObjectsProvider {
	private static final long serialVersionUID = 1L;

	private static final Class[] MEMBER_CLASSES = new Class[]{ClientGroup.class,
			Client.class};

	private Set<ClientGroup> clientGroups;
	private Set<Client> members;
	private Set<ApplicationGroup> applicationGroups;
	private Set<Application> applications;

	private String groupType;
	
	/*
	 * This method returns a set of the referenced applicationgroup.
	 * If there is none it creates a new one.
	 */
	public Set<ApplicationGroup> getApplicationGroups() {
		if (null == applicationGroups)
			applicationGroups = new HashSet<ApplicationGroup>();

		return applicationGroups;
	}
	
	/*
	 * This method returns a set of the referenced application.
	 * If there is none it creates a new one.
	 */
	public Set<Application> getApplications() {
		if (null == applications)
			applications = new HashSet<Application>();
		return applications;
	}

	/*
	 * This method sets the referenced applicationgroups by the given set of applicationgroups.
	 */
	public void setApplicationGroups(Set<ApplicationGroup> applicationGroups) {
		this.applicationGroups = applicationGroups;
		firePropertyChange("applicationGroups", null, applicationGroups);
	}

	/*
	 * This method sets the referenced applications by the given set of applications.
	 */
	public void setApplications(Set<Application> applications) {
		this.applications = applications;
		firePropertyChange("applications", null, applications);
	}

	/*
	 *This method returns a set of the referenced clientgroup.
	 * If there is none it creates a new one.
	 */
	public Set<ClientGroup> getClientGroups() {
		if (null == clientGroups)
			clientGroups = new HashSet<ClientGroup>();
		return clientGroups;
	}

	/*
	 * This method sets the referenced clientgroups by the given set of clientgroups.
	 */
	public void setClientGroups(Set<ClientGroup> clientGroups) {
		this.clientGroups = clientGroups;
		firePropertyChange("clientGroups", null, clientGroups);
	}



	/*
	 * @see
	 * org.openthinclient.common.model.AssociatedObjectsProvider#getAssociatedObjects
	 * ()
	 * This method returns all accociated objects for the directoryobject. 
	 * This means that it returns the referenced objects (like applications or thinclientgroups)
	 * for the referenced class. (For example in this case the ClientGroup) 
	 */
	public Map<Class, Set<? extends DirectoryObject>> getAssociatedObjects() {
		final Map<Class, Set<? extends DirectoryObject>> assocObjects = new HashMap<Class, Set<? extends DirectoryObject>>();
		assocObjects.put(Application.class, applications);
		assocObjects.put(ApplicationGroup.class, applicationGroups);
		assocObjects.put(ClientGroup.class, clientGroups);

		return assocObjects;
	}

	/*
	 * @see
	 * org.openthinclient.common.model.AssociatedObjectsProvider#setAssociatedObjects
	 * (java.lang.Class, java.util.Set)
	 * This method sets the referenced objects as a set. 
	 * Referenced objects could be for example a set of applications.
	 */
	public void setAssociatedObjects(Class subgroupClass,
			Set<? extends DirectoryObject> subgroups) {
		if (subgroupClass.equals(Application.class))
			setApplications((Set<Application>) subgroups);
		if (subgroupClass.equals(ApplicationGroup.class))
			setApplicationGroups((Set<ApplicationGroup>) subgroups);
		if (subgroupClass.equals(ClientGroup.class))
			setClientGroups((Set<ClientGroup>) subgroups);
	}

	/*
	 * @see java.lang.Object#toString()
	 * This method returns the thinclientgroup's name and its description as a String.
	 */
	@Override
	public String toString() {
		return new StringBuffer("[ClientGroup name=").append(getName())
				.append(", description=").append(getDescription()).append("]")
				.toString();
	}
	
	
	/*
	 * @see org.openthinclient.common.model.Group#getMemberClasses()
	 * Get the list of classes which can be members of this group.
	 */
	public Class[] getMemberClasses() {
		return MEMBER_CLASSES;
	}

	/*
	 * @see org.openthinclient.common.model.Group#getMembers()
	 * Get the set of clients which are members of this group.
	 * If there are no members return a new hashset from the type Client.
	 */
	public Set<Client> getMembers() {
		if (null == members)
			members = new HashSet<Client>();
		return members;
	}

	/*
	 * @see org.openthinclient.common.model.Group#setMembers(java.util.Set)
	 * 
	 * @deprecated for LDAP mapping only
	 * Set the memebers by the given set of clients.
	 * 
	 */
	public void setMembers(Set<Client> members) {
		this.members = members;
	}

	/*
	 * 
	 */
	public String getGroupType() {
		return groupType;
	}

	/*
	 * 
	 */
	public void setGroupType(String groupType) {
		this.groupType = groupType;
	}
}
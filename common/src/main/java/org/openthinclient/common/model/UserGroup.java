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
public class UserGroup extends DirectoryObject
		implements
			Group<User>,
			AssociatedObjectsProvider {
	private static final long serialVersionUID = 1L;

	private static final Class[] MEMBER_CLASSES = new Class[]{UserGroup.class,
			User.class};

	private String businessCategory;

	private Set<UserGroup> userGroups;
	private Set<Application> applications;
	private Set<ApplicationGroup> applicationGroups;
	private Set<Printer> printers;

	private Set<User> members;

	public Set<Application> getApplications() {
		return applications;
	}

	public void setApplications(Set<Application> applications) {
		this.applications = applications;
		firePropertyChange("applications", null, applications);
	}

	public Set<UserGroup> getUserGroups() {
		return userGroups;
	}

	public void setUserGroups(Set<UserGroup> userGroups) {
		this.userGroups = userGroups;
	}

	public Set<ApplicationGroup> getApplicationGroups() {
		return applicationGroups;
	}

	public void setApplicationGroups(Set<ApplicationGroup> applicationGroups) {
		this.applicationGroups = applicationGroups;
		firePropertyChange("applicationGroups", null, applicationGroups);
	}

	/*
	 * @see org.openthinclient.common.model.AssociatedObjectsProvider#getAssociatedObjects()
	 */
	public Map<Class, Set<? extends DirectoryObject>> getAssociatedObjects() {
		final Map<Class, Set<? extends DirectoryObject>> assocObjects = new HashMap<Class, Set<? extends DirectoryObject>>();
		assocObjects.put(Application.class, applications);
		assocObjects.put(ApplicationGroup.class, applicationGroups);
		assocObjects.put(Printer.class, printers);
		assocObjects.put(UserGroup.class, userGroups);

		return assocObjects;
	}

	/*
	 * @see org.openthinclient.common.model.AssociatedObjectsProvider#setAssociatedObjects(java.lang.Class,
	 *      java.util.Set)
	 */
	public void setAssociatedObjects(Class subgroupClass,
			Set<? extends DirectoryObject> subgroups) {
		if (subgroupClass.equals(Application.class))
			setApplications((Set<Application>) subgroups);
		if (subgroupClass.equals(ApplicationGroup.class))
			setApplicationGroups((Set<ApplicationGroup>) subgroups);
		if (subgroupClass.equals(Printer.class))
			setPrinters((Set<Printer>) subgroups);
		if (subgroupClass.equals(UserGroup.class))
			setUserGroups((Set<UserGroup>) subgroups);

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
	public Set<User> getMembers() {
		if (null == members)
			members = new HashSet<User>();
		return members;
	}

	/*
	 * @see org.openthinclient.common.model.Group#setMembers(java.util.Set)
	 * @deprecated for LDAP mapping only
	 */
	public void setMembers(Set<User> members) {
		this.members = members;
	}

	/**
	 * @return
	 */
	public String getBusinessCategory() {
		return businessCategory;
	}

	/**
	 * @param businessCategory
	 * @deprecated for thow, this is for LDAP mapping only. The business category
	 *             should not be updated.
	 */
	@Deprecated
	public void setBusinessCategory(String businessCategory) {
		this.businessCategory = businessCategory;
	}

	public Set<Printer> getPrinters() {
		return printers;
	}

	public void setPrinters(Set<Printer> printers) {
		this.printers = printers;
		firePropertyChange("printers", null, printers);
	}
}

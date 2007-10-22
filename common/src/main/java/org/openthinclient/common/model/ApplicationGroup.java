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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author levigo
 */
public class ApplicationGroup extends DirectoryObject
    implements
      Group,
      AssociatedObjectsProvider {
  private static final long serialVersionUID = 1L;

//  private static final Class[] MEMBER_CLASSES = new Class[]{ApplicationGroup.class, Client.class, User.class,UserGroup.class};
  private static final Class[] MEMBER_CLASSES = new Class[]{Client.class, User.class,UserGroup.class};
  
  private Set<ApplicationGroup> applicationGroups;
  private Set<Application> applications;

  private Set members;

  public Set<ApplicationGroup> getApplicationGroups() {
    if (null == applicationGroups)
      applicationGroups = new HashSet<ApplicationGroup>();

    return applicationGroups;
  }

  public Set<Application> getApplications() {
    if (null == applications)
      applications = new HashSet<Application>();
    return applications;
  }

  public void setApplicationGroups(Set<ApplicationGroup> applicationGroups) {
    this.applicationGroups = applicationGroups;
    firePropertyChange("applicationGroups", null, applicationGroups);
  }

  public void setApplications(Set<Application> applications) {
    this.applications = applications;
    firePropertyChange("applications", null, applications);
  }

  /*
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return new StringBuffer("[ApplicationGroup name=").append(getName())
        .append(", description=").append(getDescription()).append("]")
        .toString();
  }

  /*
   * @see org.openthinclient.common.model.DirectoryObject#getAssociatedObjects()
   */
  public Map<Class, Set<? extends DirectoryObject>> getAssociatedObjects() {
    Map<Class, Set<? extends DirectoryObject>> assocObjects = new HashMap<Class, Set<? extends DirectoryObject>>();
    assocObjects.put(Application.class, applications);
    assocObjects.put(ApplicationGroup.class, applicationGroups);
    return assocObjects;
  }

  /*
   * @see org.openthinclient.common.model.DirectoryObject#setAssociatedObjects(java.lang.Class,
   *      java.util.Set)
   */
  public void setAssociatedObjects(Class subgroupClass,
      Set<? extends DirectoryObject> subgroups) {
    if (subgroupClass.equals(Application.class)) {
      setApplications((Set<Application>) subgroups);
    }
    if (subgroupClass.equals(ApplicationGroup.class)) {
      setApplicationGroups((Set<ApplicationGroup>) subgroups);
    }

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
  public Set getMembers() {
    return members;
  }

  /*
   * @see org.openthinclient.common.model.Group#setMembers(java.util.Set)
   * @deprecated for LDAP mapping only
   */
  public void setMembers(Set members) {
    this.members = Collections.unmodifiableSet(members);
  }
  
  public void setNewMembers(Set newMembers) {
	  if(this.members == null)
		  this.members = new HashSet<DirectoryObject>();
	  for (Object member :  newMembers) {
		  this.members.add(member);
	  }	
	  
  }
}

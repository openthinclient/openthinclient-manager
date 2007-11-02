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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.castor.util.Base64Encoder;

/**
 * @author levigo
 */
public class User extends DirectoryObject implements AssociatedObjectsProvider {
  private static final long serialVersionUID = 1L;

  private Set<UserGroup> userGroups;

  private Set<ApplicationGroup> applicationGroups;
  private Set<Application> applications;
  private Set<Printer> printers;

  private Location location;
  private String stringLocation;

  private String sn;
  private String givenName;
  private byte[] userPassword;
  private String newPassword = "";
  private String verifyPassword = "";

  private Integer uid;
  
  public Set<ApplicationGroup> getApplicationGroups() {
    return applicationGroups;
  }

  public void setApplicationGroups(Set<ApplicationGroup> applicationGroups) {
    this.applicationGroups = applicationGroups;
    firePropertyChange("applicationGroups", null, applicationGroups);
  }

  public Set<Application> getApplications() {
    return applications;
  }

  public void setApplications(Set<Application> applications) {
    this.applications = applications;
    firePropertyChange("applications", null, applications);
  }

//  public Location getLocation() {
//    return location;
//  }
//  
//  public String getStringLocation() {
//	    return stringLocation;
//  }

  public void setLocation(Location location) {
    this.location = location;
    if(location == null) {
    	this.stringLocation = "";
    }else {
    	this.stringLocation = location.toString();
    } 
    firePropertyChange("location", null, location);
  }

  public Set<Printer> getPrinters() {
    return printers;
  }

  public void setPrinters(Set<Printer> printers) {
    this.printers = printers;
    firePropertyChange("printers", null, printers);
  }

  public Set<UserGroup> getUserGroups() {
    return userGroups;
  }

  public void setUserGroups(Set<UserGroup> userGroups) {
    this.userGroups = userGroups;
    firePropertyChange("userGroups", null, userGroups);
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
    firePropertyChange("givenName", null, givenName);
  }

  public String getSn() {
  	if(null == sn)
  		return this.getName();
    return sn;
  }

  public void setSn(String sn) {
    this.sn = sn;
    firePropertyChange("sn", null, sn);
  }

  /*
   * @see org.openthinclient.common.model.AssociatedObjectsProvider#getAssociatedObjects()
   */
  public Map<Class, Set<? extends DirectoryObject>> getAssociatedObjects() {
    Map<Class, Set<? extends DirectoryObject>> assocObjects = new HashMap<Class, Set<? extends DirectoryObject>>();
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
    if (subgroupClass.equals(Application.class)) {
      setApplications((Set<Application>) subgroups);
    }
    if (subgroupClass.equals(ApplicationGroup.class)) {
      setApplicationGroups((Set<ApplicationGroup>) subgroups);
    }
    if (subgroupClass.equals(Printer.class)) {
      setPrinters((Set<Printer>) subgroups);
    }

    if (subgroupClass.equals(UserGroup.class)) {
    		setUserGroups((Set<UserGroup>) subgroups);
    }
  }

  /**
   * @return
   * @deprecared Used for LDAP-Mapping only
   */
  public byte[] getUserPassword() {
    return userPassword;
  }

  /**
   * @param userPassword
   * @deprecared Used for LDAP-Mapping only
   */
  public void setUserPassword(byte[] userPassword) {
    this.userPassword = userPassword;
  }

  public String getNewPassword() {
    // we never hand out the real password, only the change one for verification
    return newPassword;
  }

  public void setNewPassword(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      digest.update(password.getBytes());
      String encrypted = "{MD5}"
          + new String(Base64Encoder.encode(digest.digest()));

      setUserPassword(encrypted.getBytes());
//      setUserPassword(password.getBytes());

      this.newPassword = password;

      firePropertyChange("newPassword", "", password);
      firePropertyChange("password", new byte[0], getUserPassword());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Can't encrypt user's password", e);
    }
  }

  public String getVerifyPassword() {
    return verifyPassword;
  }

  public void setVerifyPassword(String verifyPassword) {
    this.verifyPassword = verifyPassword;
    firePropertyChange("verifyPassword", "", verifyPassword);
  }

  public Integer getUid() {
    return uid;
  }

  public void setUid(Integer uid) {
    this.uid = uid;
  }
}

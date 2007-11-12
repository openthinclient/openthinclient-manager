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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * @author levigo
 */
public abstract class DirectoryObject implements Serializable {
  private static final long serialVersionUID = 1L;

  private String dn;
  private String name;
  private String description;

  /**
   * The realm association is transient, since it is recreated on every query.
   */
  private transient Realm realm;

  /*
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    boolean ret = (null != obj && getClass().equals(obj.getClass())
        && dn != null && dn.equals(((DirectoryObject) obj).getDn()));
    return ret;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() ^ (null != dn ? dn.hashCode() : 28764721);
  }

  public String getDn() {
    return dn;
  }

  public void setDn(String dn) {
    this.dn = dn;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    String oldDescription = this.description;
    this.description = description;
    firePropertyChange("description", oldDescription, description);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    String oldName = this.name;
    this.name = name;
    firePropertyChange("name", oldName, name);
  }

  /*
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getName();
  }

  private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  public void addPropertyChangeListener(PropertyChangeListener listener) {
	pcs.addPropertyChangeListener(listener);
  }

  public void addPropertyChangeListener(String propertyName,
      PropertyChangeListener listener) {
    pcs.addPropertyChangeListener(propertyName, listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    pcs.removePropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(String propertyName,
      PropertyChangeListener listener) {
    pcs.removePropertyChangeListener(propertyName, listener);
  }

  /**
   * Support for reporting bound property changes for Object properties. This
   * method can be called when a bound property has changed and it will send the
   * appropriate PropertyChangeEvent to any registered PropertyChangeListeners.
   * 
   * @param propertyName the property whose value has changed
   * @param oldValue the property's previous value
   * @param newValue the property's new value
   */
  protected void firePropertyChange(String propertyName, Object oldValue,
      Object newValue) {
    if (pcs == null
        || (oldValue != null && newValue != null && oldValue.equals(newValue)))
      return;
    pcs.firePropertyChange(propertyName, oldValue, newValue);
  }

  public Realm getRealm() {
    return realm;
  }

  public void setRealm(Realm realm) {
    this.realm = realm;
  }
}

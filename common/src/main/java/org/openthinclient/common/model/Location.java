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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author levigo
 */
public class Location extends Profile implements AssociatedObjectsProvider {
  private static final long serialVersionUID = 1L;

  private Set<Printer> printers;

  public Set<Printer> getPrinters() {
    return printers;
  }

  public void setPrinters(Set<Printer> printers) {
    this.printers = printers;
    firePropertyChange("printers", null, printers);
  }

  /*
   * @see org.openthinclient.common.model.AssociatedObjectsProvider#getAssociatedObjects()
   */
  public Map<Class, Set<? extends DirectoryObject>> getAssociatedObjects() {
    Map<Class, Set<? extends DirectoryObject>> subgroups = new HashMap<Class, Set<? extends DirectoryObject>>();
    subgroups.put(Printer.class, printers);
    return subgroups;
  }

  /*
   * @see org.openthinclient.common.model.AssociatedObjectsProvider#setAssociatedObjects(java.lang.Class,
   *      java.util.Set)
   */
  public void setAssociatedObjects(Class subgroupClass,
      Set<? extends DirectoryObject> subgroups) {
    if (subgroupClass.equals(Printer.class)) {
      setPrinters((Set<Printer>) subgroups);
    }

  }
}

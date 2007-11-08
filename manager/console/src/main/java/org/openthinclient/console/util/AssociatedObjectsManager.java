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
package org.openthinclient.console.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openthinclient.common.model.AssociatedObjectsProvider;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Group;


/**
 * @author Natalie Bohnert
 */
public class AssociatedObjectsManager {

  public static Map<String, Set<? extends DirectoryObject>> getAllAssociatedObjects(
      Class assocObjects, DirectoryObject objProvider, String path) {
    Map assoc = new HashMap<String, Set<? extends DirectoryObject>>();
    Map<Class, Set<? extends DirectoryObject>> ass = ((AssociatedObjectsProvider) objProvider)
        .getAssociatedObjects();
    Set<Class> classes = ass.keySet();
    for (Class assocClass : classes) {
      Set<? extends DirectoryObject> dirObjects = ass.get(assocClass);

      if (dirObjects != null) {
        for (DirectoryObject dirObject : dirObjects) {
          if (dirObject.getClass().equals(assocObjects)) {
            if (assoc.containsKey(path)) {
              Set objects = (Set) assoc.get(path);
              objects.add(dirObject);
            } else {
              Set objects = new HashSet();
              objects.add(dirObject);
              assoc.put(path, objects);
            }
          } else if (dirObject instanceof AssociatedObjectsProvider) {
            assoc.putAll(getAllAssociatedObjects(assocObjects, dirObject, path
                + dirObject.getName() + ";")); //$NON-NLS-1$
          }
        }
      }
    }
    return assoc;
  }

  public static Map<String, Set<? extends DirectoryObject>> getAllGroupMembers(
      Class assocObjects, Group group, String path) {
    Set<? extends DirectoryObject> members = group.getMembers();
    Map assoc = new HashMap<String, Set<? extends DirectoryObject>>();
    if (members != null) {
      for (DirectoryObject member : members) {
        if (member instanceof Group) {
          assoc.putAll(getAllGroupMembers(assocObjects, (Group) member, member
              .getName()
              + ";" + path)); //$NON-NLS-1$
        }
        // if(member instanceof AssociatedObjectsProvider){
        // Map<Class, Set<?extends DirectoryObject>> ass =
        // ((AssociatedObjectsProvider)member).getAssociatedObjects();
        // if(ass.containsKey(assocObjects)){
        // Set<?extends DirectoryObject> assocsGroup = ass.get(assocObjects);
        // if (assoc.containsKey(path + member.getName() + ";")) {
        // Set objects = (Set) assoc.get(path + member.getName() + ";");
        // objects.addAll(assocsGroup);
        // }else{
        // assoc.put(path + member.getName() + ";", assocsGroup);
        // }
        // }
        // }
        if (member.getClass().equals(assocObjects)) {
          if (assoc.containsKey(path)) {
            Set objects = (Set) assoc.get(path);
            objects.add(member);
          } else {
            Set objects = new HashSet();
            objects.add(member);
            assoc.put(path, objects);
          }
        }
      }
    }
    return assoc;
  }
}

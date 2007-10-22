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

import java.util.Map;
import java.util.Set;

/**
 * Interface for DirectoryObjects which have referenced objects. The methods are
 * only wrappers to get all referenced objects without knowing how much of them
 * are referenced an knowing the class of the. Used to get referenced objectzs
 * to show on GUI.
 * 
 * @author Natalie Bohnert
 */
public interface AssociatedObjectsProvider {

  public Map<Class, Set<? extends DirectoryObject>> getAssociatedObjects();

  public void setAssociatedObjects(Class subgroupClass,
      Set<? extends DirectoryObject> subgroups);

}

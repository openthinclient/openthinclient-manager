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

import java.util.Comparator;

import org.openthinclient.common.model.DirectoryObject;


/**
 * @author levigo
 */
public final class GenericDirectoryObjectComparator
    implements
      Comparator<DirectoryObject> {
  private static final GenericDirectoryObjectComparator INSTANCE = new GenericDirectoryObjectComparator();

  private GenericDirectoryObjectComparator() {
    // singleton constructor
  }

  public static GenericDirectoryObjectComparator getInstance() {
    return INSTANCE;
  }

  public int compare(DirectoryObject o1, DirectoryObject o2) {
    if (o1.getName() == null)
      if (o2.getName() == null)
        return 0;
      else
        return -1;

    if (o2.getName() == null)
      return 1;

    return o1.getName().compareTo(o2.getName());
  }
}

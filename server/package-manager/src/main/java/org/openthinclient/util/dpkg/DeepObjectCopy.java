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
package org.openthinclient.util.dpkg;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.openthinclient.pkgmgr.PackageManagerException;



public class DeepObjectCopy {
 
  public static Object clone(Object copyObject) throws PackageManagerException {
    try {
      ByteArrayOutputStream byArrOutStr = new ByteArrayOutputStream(4096);
      ObjectOutputStream objOutStr = new ObjectOutputStream(byArrOutStr);
      objOutStr.writeObject(copyObject);
      ByteArrayInputStream byArrInStr = new ByteArrayInputStream(byArrOutStr.toByteArray());
      ObjectInputStream objINStr = new ObjectInputStream(byArrInStr);
      Object deepCopy = objINStr.readObject();
      return deepCopy;
    } catch (IOException e) {
    	e.printStackTrace();
      throw new PackageManagerException(e);
    } catch(ClassNotFoundException e) {
    	e.printStackTrace();
      throw new PackageManagerException(e);
    }
  }
}


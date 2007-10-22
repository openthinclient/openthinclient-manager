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
 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.util.dpkg.Package;


/**
 * @author levigo
 */
public class DEBDumper {
  public static void main(String[] arguments) throws PackageManagerException {
    long start = System.currentTimeMillis();
    
    List<Package> packageList = new ArrayList<Package>();
    scanDebs(null, arguments, packageList);
    
    for (Package deb : packageList) 
      System.out.println("Scanned: " + deb.toString());

    System.out.println("(" + packageList.size() + " packages in "
        + (System.currentTimeMillis() - start) / 1000.0 + " seconds)");
  }

  /**
   * @param arguments
   * @param packageList
   * @throws PackageManagerException 
   */
  private static void scanDebs(File dir, String[] arguments, List<Package> packageList) throws PackageManagerException {
    for (int i = 0; i < arguments.length; i++) {
      String arg = arguments[i];

      File f = null != dir ? new File(dir, arg) : new File(arg);
      try {
        if (f.isDirectory()) {
          String debs[] = f.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
              return name.endsWith(".deb");
            }
          });
          scanDebs(f, debs, packageList);
        } else if (f.exists() && f.getName().endsWith(".deb"))
        	;
          packageList.add(new DPKGPackage(f, f.getPath()));
      } catch (FileNotFoundException e) {
      	e.printStackTrace();
      	throw new PackageManagerException(f+ " : "+e.toString());
      } catch (IOException e) {
      	e.printStackTrace();
        throw new PackageManagerException(f+ " : "+e.toString());
      }
    }
  }
}

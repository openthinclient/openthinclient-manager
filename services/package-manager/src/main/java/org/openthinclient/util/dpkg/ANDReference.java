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
 
import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.List;
import java.util.Map;
import java.util.Objects;

//import sun.nio.cs.ext.PCK;

/**
 * @author levigo
 */
public class ANDReference extends PackageReference implements Serializable {
  private static final long serialVersionUID = 3257283634670415921L;
  
  private PackageReference refs[];

  public ANDReference(String specifier) {
    // accept the empty string!
    if (specifier.trim().length() != 0) {
      String r[] = specifier.split("\\s*,\\s*");
      refs = new PackageReference[r.length];
      for (int i = 0; i < r.length; i++) {
        if (r[i].indexOf("|") >= 0)
          refs[i] = new ORReference(r[i]);
        else
          refs[i] = parse(r[i]);
      }
    } else
      refs = new PackageReference[0];
  }
  /*
   * @see org.openthinclient.util.dpkg.PackageReference#toString()
   */
  public String toString() {
    if (null == refs)
      return "";

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < refs.length; i++) {
      PackageReference ref = refs[i];
      sb.append(ref.toString());
      if (i < refs.length - 1)
        sb.append(", ");
    }

    return sb.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), refs);
  }

  @Override
  public boolean matches(Package pkg) {
    for (PackageReference r : refs) {
      if (r.matches(pkg))
        return true;
    }

    return false;
  }

  @Override
  public boolean isSatisfiedBy(Map<String, Package> pkgs) {
    for (PackageReference r : refs) {
      if(!r.isSatisfiedBy(pkgs))
        return false;
    }
    
    return true;
  }

  public PackageReference[] getRefs() {
    return refs;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    ANDReference that = (ANDReference) o;
    return Objects.deepEquals(refs, that.refs);
  }
}

/*******************************************************************************
 * openthinclient.org ThinClient suite
 * <p/>
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * <p/>
 * <p/>
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.util.dpkg;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Version;

import java.io.Serializable;
import java.util.*;

/**
 * @author levigo
 */
public abstract class PackageReference implements Serializable {

  private static final long serialVersionUID = 3977016258086907959L;

  public abstract String toString();

  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  /**
   * Check whether a package version matches this package reference.
   *
   * @param pkg
   * @return
   */
  public abstract boolean matches(Package pkg);

  /**
   * Check whether this reference is satisfied by one of the packages in the passed map.
   *
   * @param pkgs
   * @return
   */
  public abstract boolean isSatisfiedBy(Map<String, Package> pkgs);

  public enum Relation {
    EARLIER("<<"), EARLIER_OR_EQUAL("<="), EQUAL("="), LATER_OR_EQUAL(">="), LATER(">>");

    public static Relation getByTextualRepresentation(String s) {
      for (Relation r : values()) {
        if (r.textualRepresentation.equals(s))
          return r;
      }
      return null;
    }

    private final String textualRepresentation;

    Relation(String s) {
      this.textualRepresentation = s;
    }

    public String getTextualRepresentation() {
      return textualRepresentation;
    }

    @Override
    public String toString() {
      return textualRepresentation;
    }
  }

  public static class SingleReference extends PackageReference {

    private final String name;
    private final Version version;
    private final Relation relation;

    public SingleReference(String name, Relation relation, Version version) {

      if (name == null) {
        throw new IllegalArgumentException("Name must not be null.");
      }

      if (relation != null && version == null) {
        throw new IllegalArgumentException("Version must not be null if relation is specified.");
      }
      
      this.name = name;
      this.relation = relation;
      this.version = version;
    }

    public String getName() {
      return name;
    }

    public Version getVersion() {
      return version;
    }

    public Relation getRelation() {
      return relation;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder(name);
      if (null != relation) {
        sb.append(" (").append(relation).append(" ").append(version).append(")");
      }
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      SingleReference that = (SingleReference) o;
      return Objects.equals(name, that.name) && Objects.equals(version, that.version)
          && Objects.equals(relation, that.relation);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, version, relation);
    }

    @Override
    public boolean matches(Package pkg) {
      // System.out.println("packgename "+name+" version"+version);
      // System.out.println("pkgname "+pkg.getName()+" version"+pkg.getVersion());
      if (!name.equalsIgnoreCase(pkg.getName()))
        return false;
      // if (name.equals(pkg.getName()))
      // System.out.println(name +" und "+pkg.getName()+" sind gleich");
      // if there is no relation, we're already done
      if (null == relation)
        return true;

      Version v = pkg.getVersion();
      switch (relation) {
        case EARLIER:
          return v.compareTo(this.version) < 0;
        case EARLIER_OR_EQUAL:
          return v.compareTo(this.version) <= 0;
        case EQUAL:
          return v.equals(this.version);
        case LATER_OR_EQUAL:
          return v.compareTo(this.version) >= 0;
        case LATER:
          return v.compareTo(this.version) > 0;
        default:
          return false; // can't happen!
      }
    }

    @Override
    public boolean isSatisfiedBy(Map<String, org.openthinclient.pkgmgr.db.Package> pkgs) {
      Package definingPackage = pkgs.get(name);
      if (null == definingPackage)
        return false;

      // is the dependency satisfied by the package itself or by a virtual
      // package it provides?
      if (name.equals(definingPackage.getName()))
        return matches(definingPackage);

      // FIXME that implementation seems to be completely broken
      // else if (null == version) {
      // // have a look at the "provides" section, if this reference
      // // does not specify a version number
      // if (definingPackage.getProvides() instanceof ANDReference) {
      // for (PackageReference ref : ((ANDReference) definingPackage.getProvides()).getRefs())
      // if (name.equals(ref.getName()))
      // return true;
      // } else if (name.equals(definingPackage.getProvides().getName()))
      // return true;
      // }

      return false;
    }
  }

  public static class OrReference extends PackageReference {

    private final List<SingleReference> refs;

    public OrReference(SingleReference... refs) {
      this(Arrays.asList(refs));
    }

    public OrReference(List<SingleReference> refs) {
      // we're always maintaing our private copy here to ensure that there will be no external
      // modification at all
      this.refs = new ArrayList<>(refs);
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < refs.size(); i++) {
        PackageReference ref = refs.get(i);
        sb.append(ref.toString());
        if (i < refs.size() - 1)
          sb.append(" | ");
      }

      return sb.toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(refs);
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
        if (r.isSatisfiedBy(pkgs))
          return true;
      }

      return false;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      OrReference that = (OrReference) o;
      return refs.equals(that.refs);
    }

    public List<SingleReference> getReferences() {
      return Collections.unmodifiableList(refs);
    }
  }
}

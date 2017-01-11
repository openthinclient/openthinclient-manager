/*******************************************************************************
 * openthinclient.org ThinClient suite
 *
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 *
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *******************************************************************************/
package org.openthinclient.pkgmgr.db;

import java.nio.file.Path;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "otc_installation_log")
public class InstallationLogEntry {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_id")
    private Installation installation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private Package pkg;

    @Column(length = 10, columnDefinition = "char")
    @Enumerated(EnumType.STRING)
    private PackageInstalledContent.Type type;
    @Column
    private Path path;

    @Column(name="package")
    private String packageName;
    
    /**
     * Use the factory methods instead.
     */
    @Deprecated
    public InstallationLogEntry() {
    }

    InstallationLogEntry(final Installation installation, final Package pkg, final PackageInstalledContent.Type type, final Path path, final String packageName) {
        this.installation = installation;
        this.pkg = pkg;
        this.type = type;
        this.path = path;
        this.packageName = packageName;
    }

    public static InstallationLogEntry file(Installation installation, Package pkg, Path path) {
        return new InstallationLogEntry(installation, pkg, PackageInstalledContent.Type.FILE, path, pkg.getName() + " " + pkg.getVersion());
    }

    public static InstallationLogEntry symlink(Installation installation, Package pkg, Path path) {
        return new InstallationLogEntry(installation, pkg, PackageInstalledContent.Type.SYMLINK, path, pkg.getName() + " " + pkg.getVersion());
    }

    public static InstallationLogEntry dir(Installation installation, Package pkg, Path path) {
        return new InstallationLogEntry(installation, pkg, PackageInstalledContent.Type.DIR, path, pkg.getName() + " " + pkg.getVersion());
    }

    public Installation getInstallation() {
        return installation;
    }

    public Path getPath() {
        return path;
    }

    public Long getId() {
        return id;
    }

    public PackageInstalledContent.Type getType() {
        return type;
    }

    public Package getPackage() {
        return pkg;
    }

    @Override
    public String toString() {
        return "InstallationLogEntry{" +
                "type=" + type +
                ", path=" + path +
                ", packageName=" + packageName +
                '}';
    }

   /**
    * @return the packageName
    */
   public String getPackageName() {
      return packageName;
   }

   /**
    * @param packageName the packageName to set
    */
   public void setPackageName(String packageName) {
      this.packageName = packageName;
   }

}

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

import org.hibernate.annotations.GenericGenerator;

import java.nio.file.Path;

import javax.persistence.*;

@Entity
@Table(name = "otc_installation_log")
public class InstallationLogEntry {

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_id")
    private Installation installation;

    @Column(length = 10, columnDefinition = "char")
    @Enumerated(EnumType.STRING)
    private PackageInstalledContent.Type type;
    @Column
    private Path path;

    @Column(name="package_name")
    private String packageName;
    @Column(name="package_version")
    private String packageVersion;
    @Column(name="package_source_url")
    private String packageSourceUrl;
    
    /**
     * Use the factory methods instead.
     */
    @Deprecated
    public InstallationLogEntry() {
    }

    InstallationLogEntry(final Installation installation, final Package pkg, final PackageInstalledContent.Type type, final Path path) {
        this.installation = installation;
        this.type = type;
        this.path = path;
        this.packageName = pkg.getName();
        this.packageVersion  = pkg.getVersion() != null ? pkg.getVersion().toString() : "";
        this.packageSourceUrl = pkg.getSource() != null && pkg.getSource().getUrl() != null ? pkg.getSource().getUrl().toString() : "";
    }

    public static InstallationLogEntry file(Installation installation, Package pkg, Path path) {
        return new InstallationLogEntry(installation, pkg, PackageInstalledContent.Type.FILE, path);
    }

    public static InstallationLogEntry symlink(Installation installation, Package pkg, Path path) {
        return new InstallationLogEntry(installation, pkg, PackageInstalledContent.Type.SYMLINK, path);
    }

    public static InstallationLogEntry dir(Installation installation, Package pkg, Path path) {
        return new InstallationLogEntry(installation, pkg, PackageInstalledContent.Type.DIR, path);
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

    @Override
    public String toString() {
        return "InstallationLogEntry{" +
                "type=" + type +
                ", path=" + path +
                ", packageName=" + packageName +
                 ", packageVersion=" + packageVersion +
                '}';
    }

   /**
    * @return the packageName
    */
   public String getPackageName() {
      return packageName;
   }

   /**
    * @return the packageVersion
    */
   public String getPackageVersion() {
      return packageVersion;
   }

   /**
    * @return the packageSourceUrl
    */
   public String getPackageSourceUrl() {
      return packageSourceUrl;
   }

}

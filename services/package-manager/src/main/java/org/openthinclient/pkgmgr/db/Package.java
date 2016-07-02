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
 ******************************************************************************/
package org.openthinclient.pkgmgr.db;

import org.openthinclient.util.dpkg.PackageReferenceList;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "otc_package")
@Access(AccessType.FIELD)
public class Package implements Serializable, Comparable<Package> {

    private static final long serialVersionUID = 0x2d33363938363032L;
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_id")
    private Source source;

    @Column
    private long installedSize;
    @Column
    private PackageReferenceList depends = new PackageReferenceList();
    @Column
    private PackageReferenceList conflicts = new PackageReferenceList();
    @Column
    private PackageReferenceList enhances = new PackageReferenceList();
    @Column(name = "pre_depends")
    private PackageReferenceList preDepends = new PackageReferenceList();
    @Column
    private PackageReferenceList provides = new PackageReferenceList();
    @Column
    private PackageReferenceList recommends = new PackageReferenceList();
    @Column
    private PackageReferenceList replaces = new PackageReferenceList();
    @Embedded
    private Version version;
    @Column
    private String architecture;
    @Column(name = "changed_by")
    private String changedBy;
    @Column(length = 40, columnDefinition = "char")
    private String date;
    @Column
    @Lob
    private String description;
    @Column(length = 80)
    private String distribution;
    @Column
    private boolean essential;
    @Column
    private String maintainer;
    @Column
    private String name;
    @Column(length = 10, columnDefinition = "char")
    private String priority;
    @Column
    private String section;
    @Column
    private String filename;
    @Column(length = 32, columnDefinition = "char")
    private String md5sum;
    @Column
    private long size;
    @Column(name = "description_short")
    @Lob
    private String shortDescription;
    @Column
    @Lob
    private String license;

    @Column
    private boolean installed;

    public Long getId() {
        return id;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public PackageReferenceList getConflicts() {
        return conflicts;
    }

    public void setConflicts(PackageReferenceList conflicts) {
        this.conflicts = conflicts;
    }

    public PackageReferenceList getDepends() {
        return depends;
    }

    public void setDepends(PackageReferenceList depends) {
        this.depends = depends;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PackageReferenceList getPreDepends() {
        return preDepends;
    }

    public void setPreDepends(PackageReferenceList preDepends) {
        this.preDepends = preDepends;
    }

    public PackageReferenceList getProvides() {
        return provides;
    }

    public void setProvides(PackageReferenceList provides) {
        this.provides = provides;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(String s) {
        version = Version.parse(s);
    }

    /**
     * @return a String within all relevant details of a package
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("  Package: ").append(getName()).append("\n");
        sb.append("  Version: ").append(getVersion()).append("\n");
        sb.append("  Architecture: ").append(getArchitecture()).append("\n");
        sb.append("  Changed-By: ").append(getChangedBy()).append("\n");
        sb.append("  Date: ").append(getDate()).append("\n");
        sb.append("  Essential: ").append(isEssential()).append("\n");
        sb.append("  Distribution: ").append(getDistribution()).append("\n");
        sb.append("  Installed-Size: ").append(getInstalledSize()).append("\n");
        sb.append("  Maintainer: ").append(getMaintainer()).append("\n");
        sb.append("  Priority: ").append(getPriority()).append("\n");
        sb.append("  Section: ").append(getSection()).append("\n");
        sb.append("  MD5sum: ").append(getMD5sum()).append("\n");
        sb.append("  Description: \n").append(getDescription()).append("\n\n");
        sb.append("  Dependencies:\n");
        sb.append("    Depends: ").append(getDepends()).append("\n");
        sb.append("    Conflicts: ").append(getConflicts()).append("\n");
        sb.append("    Enhances: ").append(getEnhances()).append("\n");
        sb.append("    Pre-Depends: ").append(getPreDepends()).append("\n");
        sb.append("    Provides: ").append(getProvides()).append("\n");
        sb.append("    Recommends: ").append(getRecommends()).append("\n");
        sb.append("    Replaces: ").append(getReplaces()).append("\n");
        return sb.toString();
    }

    /**
     *
     * @return a string of conflicting packages
     */
    public String forConflictsToString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("  Package: ").append(getName()).append("\n");
        sb.append("  Version: ").append(getVersion()).append("\n");
        sb.append("  Conflicts: ").append(getConflicts()).append("\n");
        sb.append("  Provides: ").append(getProvides()).append("\n");
        sb.append("  Description: \n").append(getDescription());
        return sb.toString();
    }
    
    /**
    *
    * @return a string of conflicting packages
    */
   public String toStringWithNameAndVersion() {
       final StringBuilder sb = new StringBuilder();
       sb.append("Package: ").append(getName()).append(" ").append(getVersion());
       return sb.toString();
   }    

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getMD5sum() {
        return md5sum;
    }

    public void setMD5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return license text of package
     */
    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    /**
     *
     * @return the size of the packed package this information is presented by the
     *         Packages.gz file
     */
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getInstalledSize() {
        return installedSize;
    }

    public void setInstalledSize(long installedSize) {
        this.installedSize = installedSize;
    }

    /**
     *
     * @return short discription of the package, this is the first line of the
     *         description in the Packages.gz
     */
    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    /**
     *
     * @return returns the section of the package this information is presented by
     *         the Packages.gz file
     */
    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    /**
     *
     * @return the priority of the package this information is presented by the
     *         Packages.gz file
     */
    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getMaintainer() {
        return maintainer;
    }

    public void setMaintainer(String maintainer) {
        this.maintainer = maintainer;
    }

    public boolean isEssential() {
        return essential;
    }

    public void setEssential(boolean essential) {
        this.essential = essential;
    }

    public String getDistribution() {
        return distribution;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public PackageReferenceList getEnhances() {
        return enhances;
    }

    public void setEnhances(PackageReferenceList enhances) {
        this.enhances = enhances;
    }

    public PackageReferenceList getRecommends() {
        return recommends;
    }

    public void setRecommends(PackageReferenceList recommends) {
        this.recommends = recommends;
    }

    public PackageReferenceList getReplaces() {
        return replaces;
    }

    public void setReplaces(PackageReferenceList replaces) {
        this.replaces = replaces;
    }

    public int compareTo(Package o) {
        final int c1 = getName().compareTo(o.getName());
        return c1 == 0 ? getVersion().compareTo(o.getVersion()) : c1;
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

}

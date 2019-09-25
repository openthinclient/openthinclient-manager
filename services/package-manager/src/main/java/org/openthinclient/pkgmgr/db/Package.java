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

import org.hibernate.annotations.GenericGenerator;
import org.openthinclient.util.dpkg.PackageReferenceList;

import java.io.Serializable;

import javax.persistence.*;

@Entity
@Table(name = "otc_package")
@Access(AccessType.FIELD)
public class Package implements Serializable, Comparable<Package> {

    private static final long serialVersionUID = 0x2d33363938363032L;
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_id")
    private Source source;

    @Column(name = "installed_size")
    private Long installedSize;
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
    private Long size;
    @Column(name = "description_short")
    @Lob
    private String shortDescription;
    @Column
    @Lob
    private String license;

    @Column
    private boolean installed;
    @Column(name = "change_log")
    @Lob
    private String changeLog;

    @PostLoad
    public void sanitizeValues() {
      // after deserialization from the DB, the priority field contains a string with additional
      // whitespaces at the end. (Derby does that)
      // removing the whitespaces to ensure consistent values
      if (this.priority != null)
        this.priority = this.priority.trim();
    }

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

    /**
     * This method returns a 'display'-representation of version-attribute without the leading epoch indicator (i.e. '0:')
     * @return version without epoch i.e. '1.2' instead of '0:1.2'
     */
    public String getDisplayVersion() {
      return version == null ? "" : version.getUpstreamVersion().concat(version.getDebianRevision() != null ? "-" + version.getDebianRevision() : "");
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
        sb.append("  Replaces: ").append(getReplaces()).append("\n");
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

    /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((architecture == null) ? 0 : architecture.hashCode());
      result = prime * result + ((changedBy == null) ? 0 : changedBy.hashCode());
      result = prime * result + ((conflicts == null) ? 0 : conflicts.hashCode());
      result = prime * result + ((date == null) ? 0 : date.hashCode());
      result = prime * result + ((depends == null) ? 0 : depends.hashCode());
      result = prime * result + ((description == null) ? 0 : description.hashCode());
      result = prime * result + ((distribution == null) ? 0 : distribution.hashCode());
      result = prime * result + ((enhances == null) ? 0 : enhances.hashCode());
      result = prime * result + (essential ? 1231 : 1237);
      result = prime * result + ((filename == null) ? 0 : filename.hashCode());
      result = prime * result + ((license == null) ? 0 : license.hashCode());
      result = prime * result + ((maintainer == null) ? 0 : maintainer.hashCode());
      result = prime * result + ((md5sum == null) ? 0 : md5sum.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((preDepends == null) ? 0 : preDepends.hashCode());
      result = prime * result + ((priority == null) ? 0 : priority.hashCode());
      result = prime * result + ((provides == null) ? 0 : provides.hashCode());
      result = prime * result + ((recommends == null) ? 0 : recommends.hashCode());
      result = prime * result + ((replaces == null) ? 0 : replaces.hashCode());
      result = prime * result + ((section == null) ? 0 : section.hashCode());
      result = prime * result + ((shortDescription == null) ? 0 : shortDescription.hashCode());
      result = prime * result + ((size == null) ? 0 : size.hashCode());
      result = prime * result + ((source == null) ? 0 : source.hashCode());
      result = prime * result + ((version == null) ? 0 : version.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      Package other = (Package) obj;
      if (architecture == null) {
         if (other.architecture != null)
            return false;
      } else if (!architecture.equals(other.architecture))
         return false;
      if (changedBy == null) {
         if (other.changedBy != null)
            return false;
      } else if (!changedBy.equals(other.changedBy))
         return false;
      if (conflicts == null) {
         if (other.conflicts != null)
            return false;
      } else if (!conflicts.equals(other.conflicts))
         return false;
      if (date == null) {
         if (other.date != null)
            return false;
      } else if (!date.equals(other.date))
         return false;
      if (depends == null) {
         if (other.depends != null)
            return false;
      } else if (!depends.equals(other.depends))
         return false;
      if (description == null) {
         if (other.description != null)
            return false;
      } else if (!description.equals(other.description))
         return false;
      if (distribution == null) {
         if (other.distribution != null)
            return false;
      } else if (!distribution.equals(other.distribution))
         return false;
      if (enhances == null) {
         if (other.enhances != null)
            return false;
      } else if (!enhances.equals(other.enhances))
         return false;
      if (essential != other.essential)
         return false;
      if (filename == null) {
         if (other.filename != null)
            return false;
      } else if (!filename.equals(other.filename))
         return false;
      if (license == null) {
         if (other.license != null)
            return false;
      } else if (!license.equals(other.license))
         return false;
      if (maintainer == null) {
         if (other.maintainer != null)
            return false;
      } else if (!maintainer.equals(other.maintainer))
         return false;
      if (md5sum == null) {
         if (other.md5sum != null)
            return false;
      } else if (!md5sum.equals(other.md5sum))
         return false;
      if (name == null) {
         if (other.name != null)
            return false;
      } else if (!name.equals(other.name))
         return false;
      if (preDepends == null) {
         if (other.preDepends != null)
            return false;
      } else if (!preDepends.equals(other.preDepends))
         return false;
      if (priority == null) {
         if (other.priority != null)
            return false;
      } else if (!priority.equals(other.priority))
         return false;
      if (provides == null) {
         if (other.provides != null)
            return false;
      } else if (!provides.equals(other.provides))
         return false;
      if (recommends == null) {
         if (other.recommends != null)
            return false;
      } else if (!recommends.equals(other.recommends))
         return false;
      if (replaces == null) {
         if (other.replaces != null)
            return false;
      } else if (!replaces.equals(other.replaces))
         return false;
      if (section == null) {
         if (other.section != null)
            return false;
      } else if (!section.equals(other.section))
         return false;
      if (shortDescription == null) {
         if (other.shortDescription != null)
            return false;
      } else if (!shortDescription.equals(other.shortDescription))
         return false;
      if (size == null) {
       if (other.size != null)
         return false;
      } else if (!size.equals(other.size))
       return false;
      if (source == null) {
         if (other.source != null)
            return false;
      } else if (!source.equals(other.source))
         return false;
      if (version == null) {
        return other.version == null;
      } else return version.equals(other.version);
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
        return size == null ? 0 : size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getInstalledSize() {
        return installedSize == null ? 0 : installedSize;
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

   /**
    * @return the changeLog
    */
   public String getChangeLog() {
      return changeLog;
   }

   /**
    * @param changeLog the changeLog to set
    */
   public void setChangeLog(String changeLog) {
      this.changeLog = changeLog;
   }

  /**
   * Updates all metadata fields using the provided {@link Package}.
   * <b>IMPORTANT: this will not update {@link #getId() id}, {@link #getName() name},
   * {@link #getVersion() version} and state information like {@link #isInstalled() the installed state}</b>
   * @param pkg reference {@link Package} to use
   */
  public void updateFrom(Package pkg) {
    setArchitecture(pkg.getArchitecture());
    setChangedBy(pkg.getChangedBy());
    setConflicts(pkg.getConflicts());
    setDate(pkg.getDate());
    setDepends(pkg.getDepends());
    setDescription(pkg.getDescription());
    setDistribution(pkg.getDistribution());
    setEnhances(pkg.getEnhances());
    setEssential(pkg.isEssential());
    setFilename(pkg.getFilename());
    setLicense(pkg.getLicense());
    setMaintainer(pkg.getMaintainer());
    setMD5sum(pkg.getMD5sum());
    setName(pkg.getName());
    setPreDepends(pkg.getPreDepends());
    setPriority(pkg.getPriority());
    setProvides(pkg.getProvides());
    setRecommends(pkg.getRecommends());
    setReplaces(pkg.getReplaces());
    setSection(pkg.getSection());
    setShortDescription(pkg.getShortDescription());
    setSize(pkg.getSize());
    setSource(pkg.getSource());
    setVersion(pkg.getVersion());
    setChangeLog(pkg.getChangeLog());
  }
}

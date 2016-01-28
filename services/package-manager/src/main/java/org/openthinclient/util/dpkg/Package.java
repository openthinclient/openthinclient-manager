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
 ******************************************************************************/
package org.openthinclient.util.dpkg;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerException;

public abstract class Package implements Serializable, Comparable<Package> {

   private static final long serialVersionUID = 0x2d33363938363032L;
   private long installedSize;
   private PackageReference conflicts;
   private PackageReference depends;
   private PackageReference enhances;
   private PackageReference preDepends;
   private PackageReference provides;
   private PackageReference recommends;
   private PackageReference replaces;
   private PackageReference suggests;
   private Version version;
   private String architecture;
   private String changedBy;
   private String date;
   private String description;
   private String distribution;
   private boolean essential;
   private String maintainer;
   private String name;
   private String priority;
   private String section;
   private String filename;
   private String serverPath;
   private String md5sum;
   private long size;
   private String shortDescription;
   private String license;

   public void setInstalledSize(long installedSize) {
      this.installedSize = installedSize;
   }

   public void setConflicts(PackageReference conflicts) {
		this.conflicts = conflicts;
	}

   public void setDepends(PackageReference depends) {
		this.depends = depends;
	}

   public void setEnhances(PackageReference enhances) {
		this.enhances = enhances;
	}

   public void setPreDepends(PackageReference preDepends) {
		this.preDepends = preDepends;
	}

   public void setProvides(PackageReference provides) {
		this.provides = provides;
	}

   public void setRecommends(PackageReference recommends) {
		this.recommends = recommends;
	}

   public void setReplaces(PackageReference replaces) {
		this.replaces = replaces;
	}

   public void setSuggests(PackageReference suggests) {
		this.suggests = suggests;
	}

   public void setVersion(Version version) {
		this.version = version;
	}

   public PackageReference getConflicts() {
      return conflicts;
   }

   public PackageReference getDepends() {
      return depends;
   }

   public String getName() {
      return name;
   }

   public PackageReference getPreDepends() {
      return preDepends;
   }

   public PackageReference getProvides() {
      return provides;
   }

   public Version getVersion() {
      return version;
   }

	/**
	 * install a file on the given disk drive
	 * 
	 * @param file
	 * @param list
	 * @param archivesDir
	 * @throws PackageManagerException
	 */
	public abstract void install(File file, List<InstallationLogEntry> list, File archivesDir,
			PackageManager pm) throws PackageManagerException;

	/**
	 * 
	 * @return a String within all relevant details of a package
	 */
	public abstract String toString();

	/**
	 * 
	 * @return a string of conflicting packages
	 */
	public abstract String forConflictsToString();

	/**
	 * This method is used to set the serverpath it needed because there could be
	 * more then one serverpath for packages
	 * 
	 * @param s
	 */
   public void setServerPath(String s) {
      serverPath = s;
   }

   public String getServerPath() {
      return serverPath;
   }

   public String getFilename() {
      return filename;
   }

   public String getMD5sum() {
      return md5sum;
   }

	/**
	 * 
	 * @return all files which are owned by the package
	 */

	public abstract List<File> getFileList();

	/**
	 * 
	 * @return all directories which are used by the package
	 */
	public abstract List<File> getDirectoryList();

   public void setVersion(String s) {
      version = Version.parse(s);
   }

   public String getDescription() {
      return description;
   }

	/**
	 * set all files which are owned by the package
	 * 
	 * @param list
	 */
	public abstract void setFileList(List<File> list);

	/**
	 * set all directories which are used by the package
	 * 
	 * @param list
	 */
	public abstract void setDirectoryList(List<File> list);

   public void setName(String name) {
      this.name = name;
   }

	/**
	 * 
	 * @return license text of package
	 */
	public String getLicense() {
      return license;
   }

	/**
	 * 
	 * @return the size of the packed package this information is presented by the
	 *         Packages.gz file
	 */
	public long getSize() {
      return size;
   }

   public long getInstalledSize() {
      return installedSize;
   }

	/**
	 * 
	 * @return the setted direcotry for the changelogfile of the package
	 */
	public abstract String getChangelogDir();

	/**
	 * sets the local variable for the changlogDir to the given value
	 * 
	 * @param s
	 */
	public abstract void setChangelogDir(String s);

	/**
	 * 
	 * @return short discription of the package, this is the first line of the
	 *         description in the Packages.gz
	 */
	public String getShortDescription() {
      return shortDescription;
   }

	/**
	 * 
	 * @return returns the section of the package this information is presented by
	 *         the Packages.gz file
	 */
	public String getSection() {
      return section;
   }

	/**
	 * 
	 * @return the priority of the package this information is presented by the
	 *         Packages.gz file
	 */
	public String getPriority() {
      return priority;
   }

	/**
	 * is only used if the package is already "deleted"
	 * 
	 * @return the local variable from which the folder where the "removed" files
	 *         are saved
	 */
	public abstract String getoldFolder();

	/**
	 * sets the parameter for the old folder, this is used when a package is
	 * "removed" because the folder get a special name
	 * 
	 * @param rootDir
	 */
	public abstract void setoldFolder(String rootDir);

   public void setArchitecture(String architecture) {
      this.architecture = architecture;
   }

   public void setChangedBy(String changedBy) {
      this.changedBy = changedBy;
   }

   public void setDate(String date) {
      this.date = date;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public void setDistribution(String distribution) {
      this.distribution = distribution;
   }

   public void setEssential(boolean essential) {
      this.essential = essential;
   }

   public void setLicense(String license) {
      this.license = license;
   }

   public void setSize(long size) {
      this.size = size;
   }

   public void setMaintainer(String maintainer) {
      this.maintainer = maintainer;
   }

   public void setPriority(String priority) {
      this.priority = priority;
   }

   public void setSection(String section) {
      this.section = section;
   }

   public void setMd5sum(String md5sum) {
      this.md5sum = md5sum;
   }

   public void setFilename(String filename) {
      this.filename = filename;
   }

   public void setShortDescription(String shortDescription) {
      this.shortDescription = shortDescription;
   }

   public String getMd5sum() {
      return md5sum;
   }

   public String getMaintainer() {
      return maintainer;
   }

   public boolean isEssential() {
      return essential;
   }

   public String getDistribution() {
      return distribution;
   }

   public String getDate() {
      return date;
   }

   public String getChangedBy() {
      return changedBy;
   }

   public String getArchitecture() {
      return architecture;
   }

   public PackageReference getEnhances() {
      return enhances;
   }

   public PackageReference getRecommends() {
      return recommends;
   }

   public PackageReference getReplaces() {
      return replaces;
   }

   public PackageReference getSuggests() {
      return suggests;
   }
}

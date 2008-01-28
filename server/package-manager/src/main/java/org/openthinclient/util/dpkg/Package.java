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

import org.openthinclient.pkgmgr.PackageManagerException;

public interface Package extends Serializable, Comparable<Package> {
	/**
	 * 
	 * @return a PackageReference which represent the conflicts this information
	 *         is presented by the Packages.gz file
	 */
	public PackageReference getConflicts();

	/**
	 * 
	 * @return the dependencies of the package this information is presented by
	 *         the Packages.gz file
	 */
	public PackageReference getDepends();

	/**
	 * 
	 * @param s
	 * @return
	 * @throws PackageManagerException
	 */
	public List<File> getFiles(String s) throws PackageManagerException;

	/**
	 * 
	 * @return the given name of the package this information is normally
	 *         presented by the Packages.gz file otherwise {@link setName(String)}
	 */
	public String getName();

	/**
	 * 
	 * @return the predependencies of the package this information is presented by
	 *         the Packages.gz file
	 */
	public PackageReference getPreDepends();

	/**
	 * 
	 * @return a package reference which other packages are provided in this one
	 *         this information is presented by the Packages.gz file
	 */
	public PackageReference getProvides();

	/**
	 * 
	 * @return the version of the package this information is presented by the
	 *         Packages.gz file
	 */
	public Version getVersion();

	/**
	 * install a file on the given disk drive
	 * 
	 * @param file
	 * @param list
	 * @param s
	 * @throws PackageManagerException
	 */
	public void install(File file, List<InstallationLogEntry> list, String s)
			throws PackageManagerException;

	/**
	 * 
	 * @return a String within all relevant details of a package
	 */
	public String toString();

	/**
	 * 
	 * @return a string of conflicting packages
	 */
	public String forConflictsToString();

	/**
	 * This method is used to set the serverpath it needed because there could be
	 * more then one serverpath for packages
	 * 
	 * @param s
	 */
	public void setServerPath(String s);

	/**
	 * 
	 * @return the serverpath as a String
	 */
	public String getServerPath();

	/**
	 * 
	 * @return the filename of the debian file this information is presented by
	 *         the Packages.gz file
	 */
	public String getFilename();

	/**
	 * 
	 * @return String which represents the MD5Sum of the debian file which belongs
	 *         to this package this information is presented by the Packages.gz
	 *         file
	 */
	public String getMD5sum();

	/**
	 * 
	 * @return all files which are owned by the package
	 */

	public List<File> getFileList();

	/**
	 * 
	 * @return all directories which are used by the package
	 */
	public List<File> getDirectoryList();

	/**
	 * sets the version of the package
	 */
	public void setVersion(String s);

	/**
	 * 
	 * @return the complete description of the package this information is
	 *         presented by the Packages.gz file
	 */
	public String getDescription();

	/**
	 * set all files which are owned by the package
	 * 
	 * @param list
	 */
	public void setFileList(List<File> list);

	/**
	 * set all directories which are used by the package
	 * 
	 * @param list
	 */
	public void setDirectoryList(List<File> list);

	/**
	 * This is only used one time, only for the rename while "removing" the
	 * package
	 * 
	 * @param s
	 */
	public void setName(String s);

	/**
	 * 
	 * @return if the package extends the package manager or some other essential
	 *         file
	 */
	public boolean isPackageManager();

	/**
	 * 
	 * @return the size of the packed package this information is presented by the
	 *         Packages.gz file
	 */
	public long getSize();

	/**
	 * 
	 * @return the unzipped size in KB as a long value this information is
	 *         presented by the Packages.gz file
	 */
	public long getInstalledSize();

	/**
	 * 
	 * @return the setted direcotry for the changelogfile of the package
	 */
	public String getChangelogDir();

	/**
	 * sets the local variable for the changlogDir to the given value
	 * 
	 * @param s
	 */
	public void setChangelogDir(String s);

	/**
	 * 
	 * @return short discription of the package, this is the first line of the
	 *         description in the Packages.gz
	 */
	public String getShortDescription();

	/**
	 * 
	 * @return returns the section of the package this information is presented by
	 *         the Packages.gz file
	 */
	public String getSection();

	/**
	 * 
	 * @return the priority of the package this information is presented by the
	 *         Packages.gz file
	 */
	public String getPriority();

	/**
	 * is only used if the package is already "deleted"
	 * 
	 * @return the local variable from which the folder where the "removed" files
	 *         are saved
	 */
	public String getoldFolder();

	/**
	 * sets the parameter for the old folder, this is used when a package is
	 * "removed" because the folder get a special name
	 * 
	 * @param rootDir
	 */
	public void setoldFolder(String rootDir);
}

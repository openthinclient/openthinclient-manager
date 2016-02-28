/*******************************************************************************
 * openthinclient.org ThinClient suite
 * <p/>
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * <p/>
 * <p/>
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface PackageManager {

  /**
   * @return all installed Packages
   */
  public abstract Collection<Package> getInstalledPackages();

  /**
   *
   * @return all Packages which are Updateable
   */

  public abstract Collection<Package> getUpdateablePackages();

  /**
   *
   * @return all Packages which are deleted and but avialable in the cache
   */
  public abstract Collection<Package> getAlreadyDeletedPackages();

  /**
   *
   * @return the availbale diskSpace of disk on which the installation directory
   *         is
   * @throws PackageManagerException
   */
  public abstract long getFreeDiskSpace() throws PackageManagerException;

  /**
   *
   * @param collection of Packages for which the dependency should be solved
   * @return A list of all Packages which depends on the given ones. with all
   *         subdependencies!
   */
  public abstract List<Package> solveDependencies(Collection<Package> collection);

  //	/**
  //	 * find conflicts of the packages which should be installed
  //	 *
  //	 * @param list of packages which should be installed
  //	 * @return String which the existing conflicts
  //	 */
  //	public abstract String findConflicts(List<Package> list);

  //	/**
  //	 * find out if some packages could not exist if one of the given package is
  //	 * removed
  //	 *
  //	 * @param collection which should be deleted
  //	 * @return A complete list of packages which should be removed
  //	 */
  //	public abstract List<Package> isDependencyOf(Collection<Package> collection);

  //	/**
  //	 *
  //	 * @param collection of updateable packages
  //	 * @return true ONLY if it has been found correct, that the given packages
  //	 *         could be removed AND the new ones could be downloaded AND installed
  //	 *         otherwise FALSE
  //	 * @throws PackageManagerException
  //	 */
  //	public abstract boolean update(Collection<Package> collection)
  //			throws PackageManagerException;

  //	/**
  //	 * move the given packages to the cache directory also moves the NFS
  //	 * filehandels
  //	 *
  //	 * @param collection packages to delete
  //	 * @return true ONLY if all files of the packages could be moved in the cache
  //	 *         directory AND alle the nFS filehandels could be moved also
  //	 *         otherwise FALSE
  //	 * @throws IOException
  //	 * @throws PackageManagerException
  //	 */
  //	public abstract boolean delete(Collection<Package> collection)
  //			throws IOException, PackageManagerException;

  //	/**
  //	 * Downloads the files of the given packages into a cache directory , check
  //	 * their MD5-Checksums, move them to the archives directory, and install them
  //	 * first to a testinstall direcory afterwords it copy the files to the real
  //	 * install directory otherwise FALSE
  //	 *
  //	 * @param collection
  //	 * @return TRUE ONLY if all packages are downloaded AND installed properly
  //	 *         otherwise FALSE
  //	 * @throws PackageManagerException
  //	 */
  //	public abstract boolean install(Collection<Package> collection)
  //			throws PackageManagerException;

  /**
   *
   * @return a collection of packages which could be installed
   * @throws PackageManagerException
   */
  public abstract Collection<Package> getInstallablePackages()
      throws PackageManagerException;

  //	/**
  //	 *
  //	 * @param collection of packages which should be deleted
  //	 * @return TRUE ONLY if the files and their NFS filehandles could removed
  //	 *         properly otherwise FALSE
  //	 * @throws PackageManagerException
  //	 */
  //	public abstract boolean deleteOldPackages(Collection<Package> collection)
  //			throws PackageManagerException;

  /**
   * close the different databases which the packagemanger uses
   *
   * @throws PackageManagerException
   */
  public abstract void close() throws PackageManagerException;

  /**
   *
   * @param collection
   * @return TRUE only if the file could removed properly otherwise FALSE
   */
  public abstract boolean deleteDebianPackages(Collection<Package> collection);

  /**
   *
   * @param package1 the package from which the changeLogfile should be loaded
   * @return Collection of Strings which represents the changelogfile
   * @throws IOException
   */
  public abstract Collection<String> getChangelogFile(Package package1)
      throws IOException;

  //	/**
  //	 *
  //	 * @return TRUE if the Conflicts could are removeable otherwise false
  //	 */
  //	public boolean removeConflicts();

  //	/**
  //	 *
  //	 * @param installList
  //	 * @return a String of conflicts if there are some existing otherwise an empty
  //	 *         String NOT NULL!
  //	 */
  //	public String checkForAlreadyInstalled(List<Package> installList);

  //	/**
  //	 *
  //	 * @param selectedList
  //	 * @return a Collection of packages in which the conflicts are solved
  //	 */
  //	public Collection<Package> solveConflicts(Collection<Package> selectedList);

  /**
   *
   * @return the actually progress for e.g the ProgressBar
   */
  public int getActprogress();

  /**
   * set the actually progress for e.g the ProgressBar
   *
   * @param actprogress
   */
  public void setActprogress(int actprogress);

  /**
   * @return TRUE only if the complete process is done
   */
  public boolean isDone();

  /**
   * sets the isDone variable to false
   *
   */
  public void refreshIsDone();

  /**
   *
   * @return the max value of the progress for e.g the ProgressBar
   */
  public int getMaxProgress();

  /**
   *
   * @return max file size of the files which should be installed
   */
  public int[] getActMaxFileSize();

  /**
   *
   * @return the name of the package which is downloaded actually
   */
  public String getActPackName();

  /**
   * Refreshes all the values which are used for the e.g the progressbar panel
   *
   */
  public void resetValuesForDisplaying();

  /**
   * refreshes the list of solved dependencies
   *
   */
  public void refreshSolveDependencies();

  /**
   *
   * @return TRUE only if the Packages.gz file(s) could be downloaded and read
   *         properly otherwise FALSE
   * @throws PackageManagerException
   */
  public boolean updateCacheDB() throws PackageManagerException;

  /**
   * Sets a flag which is used for the describtion of the end of any progress
   * TRUE while they have been fetched the
   */
  public void setIsDoneTrue();

  /**
   * Returns the {@link PackageManagerTaskSummary summary} object with
   * collected metadata about the last request to the {@link PackageManager}.
   *
   * @return
   */
  public PackageManagerTaskSummary fetchTaskSummary();

  /**
   * Adds a warning string to the list of warnings
   *
   * @return
   */
  public boolean addWarning(String warning);

  SourceRepository getSourceRepository();

  /**
   * Create a new {@link PackageManagerOperation}.
   *
   * @return
   */
  PackageManagerOperation createOperation();

  ListenableProgressFuture<PackageManagerOperationReport> execute(PackageManagerOperation operation);

}

/*******************************************************************************
 * openthinclient.org ThinClient suite <p/> Copyright (C) 2004, 2007 levigo holding GmbH. All Rights
 * Reserved. <p/> <p/> This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version. <p/> This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details. <p/> You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageInstalledContent;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.exception.SourceIntegrityViolationException;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.util.dpkg.LocalPackageRepository;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface PackageManager {

    /**
     * Retrieves the {@link PackageManagerConfiguration associated configuration} for the given
     * package manager instance.
     *
     * @return the {@link PackageManagerConfiguration associated configuration}
     */
    PackageManagerConfiguration getConfiguration();

    /**
     * @return all installed Packages
     */
    Collection<Package> getInstalledPackages();

    /**
     * This does a simple <code>getInstallablePackages().removeAll(getInstalledPackages())</code>
     * @return All installable packages but without already installed packages.
     */
    Collection<Package> getInstallablePackagesWithoutInstalledOfSameVersion();

    /**
     * @return all Packages which are Updateable
     */

    Collection<Package> getUpdateablePackages();

    /**
     * @return the availbale diskSpace of disk on which the installation directory is
     */
    long getFreeDiskSpace() throws PackageManagerException;

    /**
     * Returns a list of installable {@linkplain Package}s with flags: installed1=false and status=ENABLED
     * @return a collection of packages which could be installed
     */
    Collection<Package> getInstallablePackages() throws PackageManagerException;

    /**
     * close the different databases which the packagemanger uses
     */
    void close() throws PackageManagerException;

    /**
     * @param package1 the package from which the changeLogfile should be loaded
     * @return Collection of Strings which represents the changelogfile
     */
    Collection<String> getChangelogFile(Package package1)
            throws IOException;

    ListenableProgressFuture<PackageListUpdateReport> updateCacheDB();

    /**
     * Returns the {@link PackageManagerTaskSummary summary} object with collected metadata about
     * the last request to the {@link PackageManager}.
     */
    PackageManagerTaskSummary fetchTaskSummary();

    /**
     * Adds a warning string to the list of warnings
     */
    boolean addWarning(String warning);

//    SourceRepository getSourceRepository();

    /**
     * Create a new {@link PackageManagerOperation}.
     */
    PackageManagerOperation createOperation();

    ListenableProgressFuture<PackageManagerOperationReport> execute(PackageManagerOperation operation);

    SourcesList getSourcesList();

    LocalPackageRepository getLocalPackageRepository();

    /**
     * Checks whether or not the given package is installable.
     */
    boolean isInstallable(Package pkg);

    /**
     * Checks whether or not the given package is already installed.
     */
    boolean isInstalled(Package pkg);

    /**
     * The source and pending packages status-attributes will be set to 'DISABLED', it will never appear on lists, but will not be deleted physically
     * @param source Source
     */
    void deleteSource(Source source) throws SourceIntegrityViolationException;

    /**
     * Save and flush source
     * @param source {@linkplain Source}
     * @return saved Source
     */
    Source saveSource(Source source);

    /**
     * Find all enabled sources
     * @return enabled Source list 
     */
    Collection<Source> findAllSources();

    /**
     * Store given sources
     * @param sources to save
     */
    void saveSources(List<Source> sources);

    /**
     * Deletes the packages to corresponding source
     * @param source - the source, which packages should be deleted
     * @return a PackageListUpdateReport
     */
    ListenableProgressFuture<PackageListUpdateReport> deleteSourcePackagesFromCacheDB(Source source);

    List<PackageInstalledContent> getInstalledPackageContents(Package pkg);

}

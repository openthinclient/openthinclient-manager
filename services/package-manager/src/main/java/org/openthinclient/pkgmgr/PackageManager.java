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
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.util.dpkg.LocalPackageRepository;

import java.io.IOException;
import java.util.Collection;

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
     *
     * @return all Packages which are Updateable
     */

    Collection<Package> getUpdateablePackages();

    /**
     * @return the availbale diskSpace of disk on which the installation directory is
     */
    long getFreeDiskSpace() throws PackageManagerException;

    /**
     *
     * @return a collection of packages which could be installed
     * @throws PackageManagerException
     */
    Collection<Package> getInstallablePackages()
            throws PackageManagerException;

    /**
     * close the different databases which the packagemanger uses
     *
     * @throws PackageManagerException
     */
    void close() throws PackageManagerException;

    /**
     *
     * @param package1 the package from which the changeLogfile should be loaded
     * @return Collection of Strings which represents the changelogfile
     * @throws IOException
     */
    Collection<String> getChangelogFile(Package package1)
            throws IOException;

    ListenableProgressFuture<PackageListUpdateReport> updateCacheDB();

    /**
     * Returns the {@link PackageManagerTaskSummary summary} object with
     * collected metadata about the last request to the {@link PackageManager}.
     *
     * @return
     */
    PackageManagerTaskSummary fetchTaskSummary();

    /**
     * Adds a warning string to the list of warnings
     *
     * @return
     */
    boolean addWarning(String warning);

    SourceRepository getSourceRepository();

    /**
     * Create a new {@link PackageManagerOperation}.
     *
     * @return
     */
    PackageManagerOperation createOperation();

    ListenableProgressFuture<PackageManagerOperationReport> execute(PackageManagerOperation operation);

    SourcesList getSourcesList();

    LocalPackageRepository getLocalPackageRepository();
}

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

import org.openthinclient.pkgmgr.db.InstallationLogEntryRepository;
import org.openthinclient.pkgmgr.db.InstallationRepository;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.util.dpkg.DPKGPackageManager;

public class PackageManagerFactory {

    /**
     * @return a new created instance of the DPKGPackageManager
     */
    public static PackageManager createPackageManager(PackageManagerConfiguration configuration, SourceRepository sourceRepository, PackageRepository packageRepository, InstallationRepository installationRepository, InstallationLogEntryRepository installationLogEntryRepository) {

        PackageManagerTaskSummary taskSummary = new PackageManagerTaskSummary();

        DPKGPackageManager dpkgPackageManager = new DPKGPackageManager(configuration, sourceRepository, packageRepository, installationRepository, installationLogEntryRepository);
        dpkgPackageManager.setTaskSummary(taskSummary);
        return dpkgPackageManager;
    }

}

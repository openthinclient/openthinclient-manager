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
package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.impl.MapDBPackageDatabase;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author tauschfn creates a new DPKGPackageManager
 */

public class PackageManagerFactory {

	private static final Logger logger = LoggerFactory.getLogger(PackageManagerFactory.class);

	/**
	 * 
	 * @return a new Created instance of the DPKGPackageManager
	 */

	public static DPKGPackageManager createPackageManager(PackageManagerConfiguration configuration, SourceRepository sourceRepository){

//		final org.openthinclient.util.dpkg.PackageDatabase.SerializationPackageDatabaseFactory packageDatabaseFactory = new org.openthinclient.util.dpkg.PackageDatabase.SerializationPackageDatabaseFactory();

		MapDBPackageDatabase.MapDBPackageDatabaseFactory packageDatabaseFactory = new MapDBPackageDatabase.MapDBPackageDatabaseFactory();

		try {

      PackageDatabase cacheDB = packageDatabaseFactory.create(configuration.getCacheDB().toPath());
      PackageDatabase archivesDB = packageDatabaseFactory.create(configuration.getArchivesDB().toPath());
      PackageDatabase installedDB = packageDatabaseFactory.create(configuration.getPackageDB().toPath());
      PackageDatabase removedDB = packageDatabaseFactory.create(configuration.getOldDB().toPath());
      PackageManagerTaskSummary taskSummary = new PackageManagerTaskSummary();

			DPKGPackageManager dpkgPackageManager = new DPKGPackageManager(cacheDB, removedDB, installedDB,
					archivesDB, configuration, packageDatabaseFactory, sourceRepository);
			dpkgPackageManager.setTaskSummary(taskSummary);
			return dpkgPackageManager;
		} catch (final IOException e) {
			logger.error("Probably there are existing Problems with the Databases", e);
		}
		return null;
	}

}

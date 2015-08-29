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

import org.openthinclient.manager.util.http.DownloadManagerFactory;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.pkgmgr.connect.PackageListDownloader;
import org.openthinclient.util.dpkg.DPKGPackageFactory;
import org.openthinclient.util.dpkg.LocalPackageList;
import org.openthinclient.util.dpkg.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * removes the actually cache database, connect to the internet load the newest
 * Packages.gz files (which are given in the sources.list file) and make a new
 * database out of the Packages which are in these files, and not already
 * installed.
 * 
 * @author tauschfn
 * 
 */
public class UpdateDatabase {

	private static final Logger LOG = LoggerFactory.getLogger(UpdateDatabase.class);
	private final PackageManagerConfiguration configuration;
	private final PackageDatabaseFactory packageDatabaseFactory;
	private final SourcesList sourcesList;
	private File cacheDatabase;
	private final File changelogDir;

	public UpdateDatabase(PackageManagerConfiguration configuration, PackageDatabaseFactory packageDatabaseFactory, SourcesList sourcesList) {
		this.configuration = configuration;
		this.cacheDatabase = configuration.getCacheDB();
		this.packageDatabaseFactory = packageDatabaseFactory;
		this.sourcesList = sourcesList;
		changelogDir = configuration.getListsDir();
	}


	private static boolean downloadChangelogFile(NetworkConfiguration.ProxyConfiguration proxyConfiguration, Package pkg,
																							 File changelogDirectory, String changeDir, PackageManagerTaskSummary taskSummary)
					throws PackageManagerException {
		try {
			final File changelogDir = new File(changelogDirectory, changeDir);

			// server path is the base url to the package repository.
			String serverPath = pkg.getServerPath();
			if (!serverPath.endsWith("/")) {
				serverPath = serverPath + "/";
			}

			if (!changelogDir.isDirectory())
				changelogDir.mkdirs();
			final File localChangelogFile = new File(changelogDir.getCanonicalPath(), pkg.getName() + ".changelog");

			DownloadManagerFactory.create(proxyConfiguration)
							.downloadTo(createPackageChangeLogURL(serverPath, pkg), localChangelogFile);

			return true;
		} catch (final Exception e) {
			if (null != taskSummary) {
				taskSummary.addWarning(e.toString());
			}
			LOG.warn("Changelog download failed.", e);
			return false;
		}
	}

	private static URL createPackageChangeLogURL(String serverPath, Package pkg) {
		try {
			return new URL(serverPath + pkg.getName() + ".changelog");
		} catch (MalformedURLException e) {
			throw new RuntimeException("Failed to access changelog due to illegal url", e);
		}
	}

	public PackageDatabase doUpdate(PackageManagerTaskSummary taskSummary, NetworkConfiguration.ProxyConfiguration proxyConfiguration)
					throws PackageManagerException {
		List<Package> packages;
		PackageDatabase packDB;
		List<LocalPackageList> updatedFiles = null;
		final PackageListDownloader seFoSeFi = new PackageListDownloader(configuration, sourcesList);
		updatedFiles = seFoSeFi.checkForNewUpdatedFiles(taskSummary);
		if (null == updatedFiles)
			throw new PackageManagerException(I18N.getMessage("interface.noFilesAvailable"));
		packages = new ArrayList<Package>();
		for (int i = 0; i < updatedFiles.size(); i++)
			try {
				final List<Package> packageList = new ArrayList<Package>();
				packageList.addAll(new DPKGPackageFactory(null)
								.getPackage(updatedFiles.get(i).getPackagesFile()));
				for (final Package pkg : packageList) {
					packages.add(pkg);
					final Package p = packages.get(packages.size() - 1);
					p.setServerPath(updatedFiles.get(i).getSource().getUrl().toExternalForm());
					final String changelogDirName = asChangelogDirectoryName(updatedFiles.get(i).getSource());
					p.setChangelogDir(changelogDirName);
					downloadChangelogFile(proxyConfiguration, pkg, this.changelogDir, changelogDirName, taskSummary);
				}
			} catch (final IOException e) {
				e.printStackTrace();
				throw new PackageManagerException(e);
			}
		packDB = null;
		if ((cacheDatabase).isFile())
			(cacheDatabase).delete();
		try {
			packDB = packageDatabaseFactory.create(cacheDatabase.toPath());
		} catch (IOException e1) {
			e1.printStackTrace();
			LOG.error("failed to open package database", e1);
			throw new PackageManagerException(e1);
		}
		for (int i = 0; i < packages.size(); i++) {
			if (!packDB.isPackageInstalled(packages.get(i).getName())) {
				packDB.addPackage(packages.get(i));
				continue;
			}
			final int n = packDB.getPackage(packages.get(i).getName()).getVersion()
							.compareTo(packages.get(i).getVersion());
			if (n == -1)
				packDB.addPackage(packages.get(i));
		}

		try {
			packDB.save();
		} catch (IOException e) {
			LOG.error("failed to save package database", e);
			throw new PackageManagerException(e);
		}
		return packDB;
	}

	private String asChangelogDirectoryName(Source source) {
		// creating the initial changelog directory as a filename constructed using the host and the realtive path to the Packages.gz (without the Packages.gz itself)
		String changelogdir = source.getUrl().getHost() + "_" + source.getUrl().getFile().replace(PackageListDownloader.PACKAGES_GZ, "");
		if (changelogdir.endsWith("/"))
			changelogdir = changelogdir.substring(0, changelogdir
							.lastIndexOf("/"));
		changelogdir = changelogdir.replace('/', '_');
		changelogdir = changelogdir.replaceAll("\\.", "_");
		changelogdir = changelogdir.replaceAll("-", "_");
		changelogdir = changelogdir.replaceAll(":", "_COLON_");
		return changelogdir;
	}
}

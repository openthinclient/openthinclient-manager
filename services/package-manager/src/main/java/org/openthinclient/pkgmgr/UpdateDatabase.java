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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openthinclient.pkgmgr.connect.ConnectToServer;
import org.openthinclient.pkgmgr.connect.SearchForServerFile;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;
import org.openthinclient.util.dpkg.PackageDatabase;
import org.openthinclient.util.dpkg.UrlAndFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static File cacheDatabase;
	private static File changelogDir;
	private static final Logger logger = LoggerFactory.getLogger(UpdateDatabase.class);
  private final PackageManagerConfiguration configuration;
	private final SourcesList sourcesList;

	public UpdateDatabase(File cacheDatabase, File chlogDir, PackageManagerConfiguration configuration, SourcesList sourcesList) {
    this(configuration, sourcesList);
    UpdateDatabase.cacheDatabase = cacheDatabase;
    changelogDir = chlogDir;
  }

	public UpdateDatabase(PackageManagerConfiguration configuration, SourcesList sourcesList) {
    this.configuration = configuration;
		this.sourcesList = sourcesList;
	}

	public org.openthinclient.pkgmgr.PackageDatabase doUpdate(boolean isStart, PackageManagerTaskSummary taskSummary, PackageManagerConfiguration.ProxyConfiguration proxyConfiguration)
			throws PackageManagerException {
		if (!isStart)
			try {
				final org.openthinclient.pkgmgr.PackageDatabase packDB = PackageDatabase.open(cacheDatabase);
				packDB.save();
				return packDB;
			} catch (final IOException e) {
				logger.error("Failed to open package database", e);
				throw new PackageManagerException(e);
			}
		else {
			List<Package> packages;
			org.openthinclient.pkgmgr.PackageDatabase packDB;
			List<UrlAndFile> updatedFiles = null;
			final SearchForServerFile seFoSeFi = new SearchForServerFile(configuration, sourcesList);
			updatedFiles = seFoSeFi.checkForNewUpdatedFiles(taskSummary);
			if (null == updatedFiles)
				throw new PackageManagerException(I18N.getMessage("interface.noFilesAvailable"));
			packages = new ArrayList<Package>();
			for (int i = 0; i < updatedFiles.size(); i++)
				try {
					final List<Package> packageList = new ArrayList<Package>();
					packageList.addAll(new DPKGPackageFactory(null)
							.getPackage(updatedFiles.get(i).getFile()));
					for (final Package pkg : packageList) {
						packages.add(pkg);
						final Package p = packages.get(packages.size() - 1);
						p.setServerPath(updatedFiles.get(i).getUrl());
						p.setChangelogDir(updatedFiles.get(i).getChangelogDir());
						downloadChangelogFile(proxyConfiguration, pkg, changelogDir, updatedFiles.get(i)
								.getChangelogDir(), taskSummary);
					}
				} catch (final IOException e) {
					e.printStackTrace();
					throw new PackageManagerException(e);
				}
			packDB = null;
			if ((cacheDatabase).isFile())
				(cacheDatabase).delete();
			try {
				packDB = PackageDatabase.open(cacheDatabase);
			} catch (IOException e1) {
				e1.printStackTrace();
				logger.error("failed to open package database", e1);
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
				logger.error("failed to save package database", e);
				throw new PackageManagerException(e);
			}
			return packDB;
		}
	}

	public org.openthinclient.pkgmgr.PackageDatabase doUpdate(DPKGPackageManager pm, PackageManagerTaskSummary taskSummary, PackageManagerConfiguration.ProxyConfiguration proxyConfiguration)
			throws PackageManagerException {
		List<Package> packages;
		org.openthinclient.pkgmgr.PackageDatabase packDB;
		List<UrlAndFile> updatedFiles = null;
		final SearchForServerFile seFoSeFi = new SearchForServerFile(configuration, sourcesList);
		updatedFiles = seFoSeFi.checkForNewUpdatedFiles(taskSummary);
		if (null == updatedFiles)
			throw new PackageManagerException(I18N.getMessage("interface.noFilesAvailable"));
		packages = new ArrayList<Package>();
		for (int i = 0; i < updatedFiles.size(); i++)
			try {
				final List<Package> packageList = new ArrayList<Package>();
				packageList.addAll(new DPKGPackageFactory(pm).getPackage(updatedFiles
						.get(i).getFile()));
				for (final Package pkg : packageList) {
					packages.add(pkg);
					final Package p = packages.get(packages.size() - 1);
					p.setServerPath(updatedFiles.get(i).getUrl());
					p.setChangelogDir(updatedFiles.get(i).getChangelogDir());
					downloadChangelogFile(proxyConfiguration, pkg, changelogDir, updatedFiles.get(i)
							.getChangelogDir(), taskSummary);
				}

			} catch (final IOException e) {
				String errormessage = I18N.getMessage("UpdateDatabase.doUpdate.GeneratePackages.IOException");
				if (null != taskSummary)
					taskSummary.addWarning(errormessage + e);
				logger.error(errormessage, e);
				e.printStackTrace();
				throw new PackageManagerException(e);
			}
		packDB = null;
		if ((cacheDatabase).isFile())
			(cacheDatabase).delete();
		try {
			packDB = PackageDatabase.open(cacheDatabase);
		} catch (IOException e1) {
			e1.printStackTrace();
			logger
					.error(
							I18N.getMessage("UpdateDatabase.doUpdate.OpenDatabase.IOException"),
							e1);
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
			logger
					.error(I18N.getMessage("UpdateDatabase.doUpdate.SaveDatabase.IOException"),
							e);
			e.printStackTrace();
		}
		return packDB;

	}

  private static boolean downloadChangelogFile(PackageManagerConfiguration.ProxyConfiguration proxyConfiguration, Package pkg,
                                               File changelogDirectory, String changeDir, PackageManagerTaskSummary taskSummary)
          throws PackageManagerException {
		boolean ret = false;
		try {
			final File changelogDir = new File(changelogDirectory,changeDir);
			String serverPath = pkg.getServerPath();
			serverPath = serverPath.substring(0, serverPath.lastIndexOf("/") + 1);
			final BufferedInputStream in = new BufferedInputStream(
					new ConnectToServer(proxyConfiguration, taskSummary)
							.getInputStream((new StringBuilder()).append(serverPath).append(
									pkg.getName()).append(".changelog").toString()));
			if (!changelogDir.isDirectory())
				changelogDir.mkdirs();
			final File rename = new File(changelogDir.getCanonicalPath(),
					(new StringBuilder()).append(pkg.getName()).append(".changelog")
							.toString());
			final FileOutputStream out = new FileOutputStream(rename);
			final byte buf[] = new byte[4096];
			int len;
			while ((len = in.read(buf)) > 0)
				out.write(buf, 0, len);
			out.close();
			in.close();
			ret = true;
		} catch (final Exception e) {
			if (null != taskSummary) {
				taskSummary.addWarning(e.toString());
			}
			logger.warn("Changelog download failed.", e);
		}
		return ret;
	}
}

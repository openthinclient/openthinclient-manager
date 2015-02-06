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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.openthinclient.pkgmgr.connect.InitProperties;
import org.openthinclient.pkgmgr.connect.ProxyManager;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.PackageDatabase;

import com.levigo.util.preferences.PreferenceStoreHolder;
import com.levigo.util.preferences.PropertiesPreferenceStore;

/**
 * @author tauschfn creates a new DPKGPackageManager
 */

public class PackageManagerFactory {

	private static String tempStoreName = "tempPackageManager";
	private static final Logger logger = Logger.getLogger(PackageManagerFactory.class);

	/**
	 * 
	 * @return a new Created instance of the DPKGPackageManager
	 */

	public static DPKGPackageManager getPackageManager(){
    PackageDatabase installedDB;
		PackageDatabase removedDB;
		PackageDatabase cacheDB;
		PackageDatabase archivesDB;
		try {
			InitProperties.ensurePropertiesInitialized();
			initWarningProperties();
		} catch (PackageManagerException e) {
			logger.error("Could not initialize the Properties of the PackageManager please check this.",e);
			e.printStackTrace();
		}
		new ProxyManager().checkForProxy();
		checkIfDirectoriesAreCreated(PreferenceStoreHolder
				.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
						"archivesDir", null), PreferenceStoreHolder
				.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
						"partialDir", null), PreferenceStoreHolder
				.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
						"testinstallDir", null), PreferenceStoreHolder
				.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
						"listsDir", null), PreferenceStoreHolder.getPreferenceStoreByName(
				tempStoreName).getPreferenceAsString("installOldDir", null),
				PreferenceStoreHolder.getPreferenceStoreByName(tempStoreName)
						.getPreferenceAsString("packageDB", null));
		// verifyPackackeManagerVersion();
		installedDB = null;
		removedDB = null;
		cacheDB = null;
		archivesDB = null;
		Collection<String> locations;
		System.out.println("proxySet"+System.getProperty("proxySet"));
		System.out.println("http.proxyHost"+System.getProperty("http.proxyHost"));
		System.out.println("http.proxyPort"+System.getProperty("http.proxyPort"));
		boolean removeItReally;
		try {

			archivesDB = PackageDatabase.open(new File(PreferenceStoreHolder
					.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
							"archivesDB", null)));
			installedDB = PackageDatabase.open(new File(PreferenceStoreHolder
					.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
							"packageDB", null)));
			removedDB = PackageDatabase.open(new File(PreferenceStoreHolder
					.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
							"oldDB", null)));
			PackageManagerTaskSummary taskSummary = new PackageManagerTaskSummary();
			try {
				cacheDB = new UpdateDatabase(PreferenceStoreHolder
						.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
								"cacheDB", null), PreferenceStoreHolder.getPreferenceStoreByName(
						tempStoreName).getPreferenceAsString("listsDir", null))
						.doUpdate(false, taskSummary);
			} catch (PackageManagerException e) {
				logger.error("Unable to create the Cache Database!",e);
				taskSummary.addWarning("Unable to create the Cache Database! Cause: " + e.getMessage());
			}
			locations = new ArrayList<String>();
			addLocation("installDir", locations);
			addLocation("archivesDir", locations);
			addLocation("testinstallDir", locations);
			addLocation("installOldDir", locations);
			addLocation("listsDir", locations);
			removeItReally = Boolean.valueOf(
					PreferenceStoreHolder.getPreferenceStoreByName("PackageManager")
							.getPreferenceAsString("removeItReally", "false")).booleanValue();
			DPKGPackageManager dpkgPackageManager = new DPKGPackageManager(cacheDB, removedDB, installedDB,
					archivesDB, locations, removeItReally);
			dpkgPackageManager.setTaskSummary(taskSummary);
			return dpkgPackageManager;
		} catch (final IOException e) {
			logger.error("Probably there are existing Problems with the Databases",e);
			e.printStackTrace();
		}
		return null;
//			throw new PackageManagerException(e);

	}

	/**
	 * add the different paths to the which are standing in the PreferenceStrore
	 * behind the Synonyme "what" to the locations Collection
	 * 
	 * @param what
	 * @param locations
	 */
	private static void addLocation(String what, Collection<String> locations) {
		final String value = PreferenceStoreHolder.getPreferenceStoreByName(
				tempStoreName).getPreferenceAsString(what, null);
		if (value.length() > 1)
			locations.add(value);
	}

	/**
	 * 
	 * @return TRUE only if the properties file could be loaded correct otherwise
	 *         false
	 * @throws FileNotFoundException
	 */
	



	/**
	 * verifies that the actually created PackageManager is also set in the
	 * installed database and that there are no old files from the package manager
	 * which should be deleted
	 * !!Actually not in Use!!!
	 * 
	 * @throws PackageManagerException
	 */
//	@SuppressWarnings("unchecked")
//	private static void verifyPackackeManagerVersion()
//			throws PackageManagerException {
//		InputStream stream = null;
//		try {
//			if ((new File((new StringBuilder()).append(programRootDirectory).append(
//					File.separator).append("temp").append(File.separator).append(
//					"package.txt").toString())).length() != 0L)
//				stream = new FileInputStream(new File((new StringBuilder()).append(
//						programRootDirectory).append(File.separator).append("temp").append(
//						File.separator).append("package.txt").toString()));
//			if (stream == null && (new File("package_manager.info")).length() != 0L)
//				stream = new FileInputStream("package_manager.info");
//			if (stream == null) {
//				final ClassLoader aClassLoader = PreferenceStoreHolder.class
//						.getClassLoader();
//				if (aClassLoader == null)
//					stream = ClassLoader
//							.getSystemResourceAsStream("package_manager.info");
//				else
//					stream = aClassLoader.getResourceAsStream("package_manager.info");
//			}
//			if (stream != null) {
//				final List<Package> packageList = new ArrayList<Package>();
//				packageList.addAll(new DPKGPackageFactory().getPackage(stream));
//				final PackageDatabase pdb = PackageDatabase.open(new File(
//						PreferenceStoreHolder.getPreferenceStoreByName(tempStoreName)
//								.getPreferenceAsString("packageDB",
//										"No entry found for packageDB")));
//				if (pdb.isPackageInstalled(packageList.get(0).getName())) {
//					final String name = packageList.get(0).getName();
//					if (!pdb.getPackage(name).getVersion().equals(
//							packageList.get(0).getVersion())) {
//						pdb.removePackage(pdb.getPackage(packageList.get(0).getName()));
//						pdb.addPackage(packageList.get(0));
//						pdb.save();
//						final ArrayList filesToDelete = new ArrayList();
//						final File array[] = (new File((new StringBuilder()).append(
//								programRootDirectory).append("package-manager-update")
//								.toString())).listFiles();
//						if (null != array && array.length != 0) {
//							for (final File file : array)
//								filesToDelete.add(file);
//
//							filesToDelete.add(new File((new StringBuilder()).append(
//									programRootDirectory).append("package-manager-update")
//									.toString()));
//							filesToDelete.add(new File((new StringBuilder()).append(
//									programRootDirectory).append(File.separator).append("temp")
//									.append(File.separator).toString()));
//							deleteForPackageManager(filesToDelete);
//						}
//					}
//				} else {
//					pdb.addPackage(packageList.get(0));
//					pdb.save();
//					final ArrayList filesToDelete = new ArrayList();
//					final File arr[] = (new File((new StringBuilder()).append(
//							programRootDirectory).append("package-manager-update").toString()))
//							.listFiles();
//					if (null != arr && arr.length != 0) {
//						for (final File file : arr)
//							filesToDelete.add(file);
//						filesToDelete.add(new File((new StringBuilder()).append(
//								programRootDirectory).append("package-manager-update")
//								.toString()));
//						filesToDelete.add(new File((new StringBuilder()).append(
//								programRootDirectory).append(File.separator).append("temp")
//								.append(File.separator).toString()));
//						deleteForPackageManager(filesToDelete);
//					}
//				}
//				pdb.close();
//			} else
//				throw new PackageManagerException(
//						"FATAL ERROR while installing the Package Manager");
//			stream.close();
//		} catch (final IOException e) {
//			e.printStackTrace();
//			throw new PackageManagerException(e);
//		}
//	}

	/**
	 * delete the given List of directories, only if they are empty
	 * !! Actaully not in USE !!
	 * 
	 * @param directories which should be deleted
	 */
//	private static void deleteForPackageManager(ArrayList<File> directory) {
//		final ArrayList<File> otherDirectories = new ArrayList<File>();
//		final Iterator it = directory.iterator();
//		do {
//			if (!it.hasNext())
//				break;
//			final File file = (File) it.next();
//			if (file.isFile())
//				file.delete();
//			if (file.isDirectory())
//				if (file.listFiles().length == 0)
//					file.delete();
//				else {
//					final File arr[] = file.listFiles();
//					final int len = arr.length;
//					for (int i = 0; i < len; i++) {
//						final File fi = arr[i];
//						otherDirectories.add(fi);
//					}
//					otherDirectories.add(file);
//				}
//		} while (true);
//		if (!otherDirectories.isEmpty())
//			deleteForPackageManager(otherDirectories);
//	}

	/**
	 * checks if the given Strings are created directories if not these
	 * directories will be created
	 * 
	 * @param archivesDir
	 * @param partialDir
	 * @param testinstallDir
	 * @param listsDir
	 * @param oldDir
	 * @param packagesDB
	 */
	private static void checkIfDirectoriesAreCreated(String archivesDir,
			String partialDir, String testinstallDir, String listsDir, String oldDir,
			String packagesDB) {
		if (!(new File(archivesDir)).isDirectory()) {
			(new File(partialDir)).mkdirs();
			(new File(testinstallDir)).mkdirs();
		} else {
			if (!(new File(partialDir)).isDirectory())
				(new File(partialDir)).mkdirs();
			if (!(new File(testinstallDir)).isDirectory())
				(new File(testinstallDir)).mkdirs();
		}
		if (!(new File(listsDir)).isDirectory())
			(new File(listsDir)).mkdirs();
		if (!(new File(oldDir)).isDirectory())
			(new File(oldDir)).mkdirs();
		if (!(new File(packagesDB)).isFile())
			(new File(packagesDB.substring(0, packagesDB.lastIndexOf(File.separator))))
					.mkdirs();
	}

	/**
	 * Load the differnt user warnings which could be given at the different
	 * points
	 * 
	 * @return TRUE only if the properties file is loaded correct
	 * @throws PackageManagerException
	 */
	@SuppressWarnings("unchecked")
	private static boolean initWarningProperties() throws PackageManagerException {
		try {
			final PropertiesPreferenceStore prefStore = new PropertiesPreferenceStore();
			prefStore.load("ScreenOutput.properties", PreferenceStoreHolder.class);
			PreferenceStoreHolder.addPreferenceStoreByName("Screen", prefStore);
			return true;
		} catch (final IOException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		}
	}
}

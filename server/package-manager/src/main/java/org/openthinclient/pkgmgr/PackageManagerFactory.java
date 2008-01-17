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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;
import org.openthinclient.util.dpkg.PackageDatabase;

import com.levigo.util.preferences.PreferenceStoreHolder;
import com.levigo.util.preferences.PropertiesPreferenceStore;

/**
 * @author tauschfn creates a new DPKGPackageManager
 */

public class PackageManagerFactory {
	private static PreferenceStoreHolder prefStHo = PreferenceStoreHolder
			.getInstance();
	private static String programRootDirectory;
	private static String tempStoreName = "tempPackageManager";

	/**
	 * 
	 * @return a new Created instance of the DPKGPackageManager
	 */
	public static DPKGPackageManager getServerPackageManager() {

		programRootDirectory = System.getProperty("jboss.server.data.dir");
		try {
			return getPackageManager();
		} catch (final PackageManagerException e) {

			// here the textually output is OK because no Preference Store Holder or
			// sth. like that could be loaded!
			System.err
					.println("Problems while creating the PackageManager, unable to solve them!");
			e.printStackTrace();
			return null; // FIXME: exception propagieren?
		}
	}

	public static PackageManager getLocalPackageManager() {
		return null;
	}

	private static DPKGPackageManager getPackageManager()
			throws PackageManagerException {
		PackageDatabase installedDB;
		PackageDatabase removedDB;
		PackageDatabase cacheDB;
		PackageDatabase archivesDB;
		initProperties();
		initWarningProperties();

		checkIfDirectorysAreCreated(PreferenceStoreHolder.getPreferenceStoreByName(
				tempStoreName).getPreferenceAsString("archivesDir", null),
				PreferenceStoreHolder.getPreferenceStoreByName(tempStoreName)
						.getPreferenceAsString("partialDir", null), PreferenceStoreHolder
						.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
								"testinstallDir", null), PreferenceStoreHolder
						.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
								"listsDir", null), PreferenceStoreHolder
						.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
								"installOldDir", null), PreferenceStoreHolder
						.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
								"packageDB", null));
		// verifyPackackeManagerVersion();
		installedDB = null;
		removedDB = null;
		cacheDB = null;
		archivesDB = null;
		Collection<String> locations;
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
			cacheDB = new UpdateDatabase(PreferenceStoreHolder
					.getPreferenceStoreByName(tempStoreName).getPreferenceAsString(
							"cacheDB", null), PreferenceStoreHolder.getPreferenceStoreByName(
					tempStoreName).getPreferenceAsString("listsDir", null))
					.doUpdate(null);
			locations = new ArrayList<String>();
			addLocation("installDir", locations);
			addLocation("archivesDir", locations);
			addLocation("testinstallDir", locations);
			addLocation("installOldDir", locations);
			addLocation("listsDir", locations);
			removeItReally = Boolean.valueOf(
					PreferenceStoreHolder.getPreferenceStoreByName("PackageManager")
							.getPreferenceAsString("removeItReally", "false")).booleanValue();
			return new DPKGPackageManager(cacheDB, removedDB, installedDB,
					archivesDB, locations, removeItReally);
		} catch (final IOException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		}
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
	private static boolean initProperties() throws PackageManagerException {
		String propertiesFileName;
		String configDir;
		PropertiesPreferenceStore prefStore;
		propertiesFileName = "package_manager.properties";
		configDir = new File(programRootDirectory + File.separator + "nfs"
				+ File.separator + "root" + File.separator + "etc" + File.separator)
				.getPath();
		prefStore = new PropertiesPreferenceStore();
		try {
			InputStream stream = null;
			if ((new File(configDir, propertiesFileName)).isFile()
					&& (new File(configDir, propertiesFileName)).length() != 0L)
				stream = new FileInputStream(new File(configDir, propertiesFileName));
			if (stream == null) {
				final ClassLoader aClassLoader = PreferenceStoreHolder.class
						.getClassLoader();
				if (aClassLoader == null)
					stream = ClassLoader.getSystemResourceAsStream(propertiesFileName);
				else
					stream = aClassLoader.getResourceAsStream(propertiesFileName);
				if (stream == null) {
					final Class aClass = PreferenceStoreHolder.class;
					stream = aClass.getResourceAsStream(propertiesFileName);
				}
				if (stream == null)
					if ((new File(propertiesFileName)).length() != 0L)
						stream = new FileInputStream(propertiesFileName);
					else
						throw new PackageManagerException("FATAL ERROR the file "
								+ propertiesFileName + " which should be located in the "
								+ configDir + " could not be loaded");
			}
			if (stream != null) {
				prefStore.load(stream);
				PreferenceStoreHolder.addPreferenceStoreByName("PackageManager",
						prefStore);
				stream.close();
				final PropertiesPreferenceStore tempPrefStore = new PropertiesPreferenceStore();
				tempPrefStore.putPreference("installDir", (new StringBuilder()).append(
						getRealPath(programRootDirectory, prefStHo.getPreferenceAsString(
								"PackageManager", "installDir", null))).append(File.separator)
						.toString());
				tempPrefStore.putPreference("workingDir", (new StringBuilder()).append(
						getRealPath(programRootDirectory, prefStHo.getPreferenceAsString(
								"PackageManager", "workingDir", null))).append(File.separator)
						.toString());
				tempPrefStore.putPreference("archivesDir", (new StringBuilder())
						.append(
								getRealPath(programRootDirectory, prefStHo
										.getPreferenceAsString("PackageManager", "archivesDir",
												null))).append(File.separator).toString());
				tempPrefStore.putPreference("testinstallDir", (new StringBuilder())
						.append(
								getRealPath(programRootDirectory, prefStHo
										.getPreferenceAsString("PackageManager", "testinstallDir",
												null))).append(File.separator).toString());
				tempPrefStore.putPreference("partialDir", (new StringBuilder()).append(
						getRealPath(programRootDirectory, prefStHo.getPreferenceAsString(
								"PackageManager", "partialDir", null))).append(File.separator)
						.toString());
				tempPrefStore.putPreference("listsDir", (new StringBuilder()).append(
						getRealPath(programRootDirectory, prefStHo.getPreferenceAsString(
								"PackageManager", "listsDir", null))).append(File.separator)
						.toString());
				tempPrefStore.putPreference("packageDB", getRealPath(
						programRootDirectory, prefStHo.getPreferenceAsString(
								"PackageManager", "packageDB", null)));
				tempPrefStore.putPreference("cacheDB", getRealPath(
						programRootDirectory, prefStHo.getPreferenceAsString(
								"PackageManager", "cacheDB", null)));
				tempPrefStore.putPreference("sourcesList", getRealPath(
						programRootDirectory, prefStHo.getPreferenceAsString(
								"PackageManager", "sourcesList", null)));
				tempPrefStore.putPreference("installOldDir", getRealPath(
						programRootDirectory, prefStHo.getPreferenceAsString(
								"PackageManager", "installOldDir", null)));
				tempPrefStore.putPreference("oldDB", getRealPath(programRootDirectory,
						prefStHo.getPreferenceAsString("PackageManager", "oldDB", null)));
				tempPrefStore.putPreference("removeItReally", getRealPath(
						programRootDirectory, prefStHo.getPreferenceAsString(
								"PackageManager", "removeItReally", null)));
				tempPrefStore.putPreference("archivesDB", getRealPath(
						programRootDirectory, prefStHo.getPreferenceAsString(
								"PackageManager", "archivesDB", null)));
				if (prefStHo.isAccessible())
					PreferenceStoreHolder.removePreferenceStore(tempStoreName);
				PreferenceStoreHolder.addPreferenceStoreByName(tempStoreName,
						tempPrefStore);
			}
			return true;
		} catch (final IOException x) {
			x.printStackTrace();
			throw new PackageManagerException(x);
		}
	}

	/**
	 * put both given strings together, check if it's an existing file an return
	 * the canonical path of it
	 * 
	 * @param programRootDirectory
	 * @param path
	 * @return the canonical path
	 * @throws IOException
	 */
	private static String getRealPath(String programRootDirectory, String path)
			throws IOException {
		File f = new File(path);
		if (!f.isAbsolute())
			f = new File(programRootDirectory, path);
		return f.getAbsoluteFile().getCanonicalPath();
	}

	/**
	 * verifys that the acually created PackageManager is also set in the
	 * installed database and that there are no old files from the package manager
	 * which should be deleted
	 * 
	 * @throws PackageManagerException
	 */
	@SuppressWarnings("unchecked")
	private static void verifyPackackeManagerVersion()
			throws PackageManagerException {
		InputStream stream = null;
		try {
			if ((new File((new StringBuilder()).append(programRootDirectory).append(
					File.separator).append("temp").append(File.separator).append(
					"package.txt").toString())).length() != 0L)
				stream = new FileInputStream(new File((new StringBuilder()).append(
						programRootDirectory).append(File.separator).append("temp").append(
						File.separator).append("package.txt").toString()));
			if (stream == null && (new File("package_manager.info")).length() != 0L)
				stream = new FileInputStream("package_manager.info");
			if (stream == null) {
				final ClassLoader aClassLoader = PreferenceStoreHolder.class
						.getClassLoader();
				if (aClassLoader == null)
					stream = ClassLoader
							.getSystemResourceAsStream("package_manager.info");
				else
					stream = aClassLoader.getResourceAsStream("package_manager.info");
			}
			if (stream != null) {
				final List<Package> packageList = new ArrayList<Package>();
				packageList.addAll(new DPKGPackageFactory().getPackage(stream));
				final PackageDatabase pdb = PackageDatabase.open(new File(
						PreferenceStoreHolder.getPreferenceStoreByName(tempStoreName)
								.getPreferenceAsString("packageDB",
										"No entry found for packageDB")));
				if (pdb.isPackageInstalled(packageList.get(0).getName())) {
					final String name = packageList.get(0).getName();
					if (!pdb.getPackage(name).getVersion().equals(
							packageList.get(0).getVersion())) {
						pdb.removePackage(pdb.getPackage(packageList.get(0).getName()));
						pdb.addPackage(packageList.get(0));
						pdb.save();
						final ArrayList filesToDelete = new ArrayList();
						final File array[] = (new File((new StringBuilder()).append(
								programRootDirectory).append("package-manager-update")
								.toString())).listFiles();
						if (null != array && array.length != 0) {
							for (final File file : array)
								filesToDelete.add(file);

							filesToDelete.add(new File((new StringBuilder()).append(
									programRootDirectory).append("package-manager-update")
									.toString()));
							filesToDelete.add(new File((new StringBuilder()).append(
									programRootDirectory).append(File.separator).append("temp")
									.append(File.separator).toString()));
							deleteForPackageManager(filesToDelete);
						}
					}
				} else {
					pdb.addPackage(packageList.get(0));
					pdb.save();
					final ArrayList filesToDelete = new ArrayList();
					final File arr[] = (new File((new StringBuilder()).append(
							programRootDirectory).append("package-manager-update").toString()))
							.listFiles();
					if (null != arr && arr.length != 0) {
						for (final File file : arr)
							filesToDelete.add(file);
						filesToDelete.add(new File((new StringBuilder()).append(
								programRootDirectory).append("package-manager-update")
								.toString()));
						filesToDelete.add(new File((new StringBuilder()).append(
								programRootDirectory).append(File.separator).append("temp")
								.append(File.separator).toString()));
						deleteForPackageManager(filesToDelete);
					}
				}
				pdb.close();
			} else
				throw new PackageManagerException(
						"FATAL ERROR while installing the Package Manager");
			stream.close();
		} catch (final IOException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		}
	}

	/**
	 * delete the given List of directorys, only if they are empty
	 * 
	 * @param directorys which should be deleted
	 */
	private static void deleteForPackageManager(ArrayList<File> directory) {
		final ArrayList<File> anotherdirectorys = new ArrayList<File>();
		final Iterator it = directory.iterator();
		do {
			if (!it.hasNext())
				break;
			final File file = (File) it.next();
			if (file.isFile())
				file.delete();
			if (file.isDirectory())
				if (file.listFiles().length == 0)
					file.delete();
				else {
					final File arr[] = file.listFiles();
					final int len = arr.length;
					for (int i = 0; i < len; i++) {
						final File fi = arr[i];
						anotherdirectorys.add(fi);
					}
					anotherdirectorys.add(file);
				}
		} while (true);
		if (!anotherdirectorys.isEmpty())
			deleteForPackageManager(anotherdirectorys);
	}

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
	private static void checkIfDirectorysAreCreated(String archivesDir,
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

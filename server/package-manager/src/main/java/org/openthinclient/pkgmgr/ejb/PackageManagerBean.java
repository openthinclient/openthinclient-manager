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
package org.openthinclient.pkgmgr.ejb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.log4j.Logger;
import org.jboss.annotation.ejb.RemoteBinding;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;

import com.levigo.util.preferences.PreferenceStoreHolder;

/**
 * This is the Interface between the "real" Package Manager, the NFSServices and
 * the GUI. Every interaction is started in here, this is the one and only class
 * in the whole PackageMAnager Package which is able to connect to the different
 * Services of the JBoss for example to the NFSService. It implements the
 * PackageManager and is also the exclusive owner of an DPKGPackageManager.
 * Which is created by the PackageManagerFactory which is exclusively used by
 * this bean class.
 * 
 * @author tauschfn
 * 
 */
@Stateless
@RemoteBinding(jndiBinding = "PackageManagerBean/remote")
@Remote(PackageManager.class)
public class PackageManagerBean implements PackageManager

{

	private static final Logger logger = Logger
			.getLogger(PackageManagerBean.class);
	private static final DPKGPackageManager delegate = PackageManagerFactory
			.getServerPackageManager();
	private List<String> warnings;

	@PostConstruct
	public void init() {
		try {
			if (null == delegate)
				throw new PackageManagerException(
						"Not Possible to create an Instance of PackageManager!");
		} catch (final PackageManagerException e) {
			e.printStackTrace();
			logger.error(e.toString());
			throw new RuntimeException(e);
		}
	}

	// @PreDestroy
	// @PreRemove
	// @PostRemove
	// @PrePassivate
	public void deinit() {
		// System.out.println("puhhh destroy IT!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		// try {
		// delegate.close();
		// } catch (PackageManagerException e) {
		// logger.error(e.toString());
		// throw new RuntimeException(e);
		// }
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#checkForAlreadyInstalled(java.util.List)
	 */
	public String checkForAlreadyInstalled(List<Package> installList) {
		return delegate.checkForAlreadyInstalled(installList);
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#close()
	 */
	public void close() throws PackageManagerException {
		delegate.close();
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#delete(java.util.Collection)
	 */
	public boolean delete(Collection<Package> deleteList) throws IOException,
			PackageManagerException {
		// for (Package pkg : deleteList)
		// System.out.println(pkg.getName());
		boolean ret = false;
		try {
			if (MBeanServerFactory.findMBeanServer(null).get(0).isRegistered(
					new ObjectName("tcat:service=NFSService")))
				ret = startDelete(deleteList);
			else if (delegate.delete(deleteList))
				if (delegate.removePackagesFromInstalledDB(new ArrayList<Package>(
						deleteList))) {
					delegate.setActprogress(new Double(delegate.getMaxProgress() * 0.95)
							.intValue());
					delegate.setActprogress(new Double(delegate.getMaxProgress())
							.intValue());
					delegate.setIsDoneTrue();
					ret = true;
				} else
					throw new PackageManagerException(
							PreferenceStoreHolder
									.getPreferenceStoreByName("Screen")
									.getPreferenceAsString(
											"PackageManagerBean.doNFSremove.removedDB",
											"No entry found for PackageManagerBean.doNFSremove.removedDB"));
		} catch (final MalformedObjectNameException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		} catch (final NullPointerException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		}

		delegate.setActprogress(delegate.getMaxProgress());
		delegate.setIsDoneTrue();
		return ret;

	}

	/**
	 * @param deleteList
	 * @return True only if the Packages could be moved (normally by the
	 *         NFSService) and the old directories could be removed otherwise the
	 *         return value is FALSE
	 * @throws IOException
	 * @throws PackageManagerException
	 */
	private boolean startDelete(Collection<Package> deleteList)
			throws IOException, PackageManagerException {
		final List<Package> pkgs = new ArrayList<Package>(delegate
				.filesToRename(deleteList));

		if (doNFSmove(delegate.getFromToFileMap())) {
			delegate.setActprogress(delegate.getActprogress()
					+ new Double(delegate.getMaxProgress() * 0.2).intValue());
			if (delegate.saveChangesInDB(pkgs)) {
				delegate.setActprogress(delegate.getActprogress()
						+ new Double(delegate.getMaxProgress() * 0.2).intValue());
				if (callDeleteNFS(delegate.getRemoveDirectoryList()))
					return true;
				else {
					final StringBuffer sb = new StringBuffer();
					for (final File fi : delegate.getRemoveDirectoryList())
						sb.append(fi.getPath());
					throw new PackageManagerException(
							PreferenceStoreHolder
									.getPreferenceStoreByName("Screen")
									.getPreferenceAsString(
											"PackageManagerBean.doNFSremove.NFSProblem",
											"No entry found for PackageManagerBean.doNFSremove.NFSProblem")
									+ " \n" + sb.toString());
				}
			} else
				throw new PackageManagerException(PreferenceStoreHolder
						.getPreferenceStoreByName("Screen").getPreferenceAsString(
								"PackageManagerBean.delete.DBtransfer",
								"No entry found for PackageManagerBean.delete.DBtransfer"));
		} else {
			callDeleteNFS(selectDirectories(delegate.getFromToFileMap().values()));
			throw new PackageManagerException(PreferenceStoreHolder
					.getPreferenceStoreByName("Screen").getPreferenceAsString(
							"PackageManagerBean.delete.doNFSmoveFailed",
							"No entry found for PackageManagerBean.delete.doNFSmoveFailed"));

		}
	}

	/**
	 * @param filesAndDirs
	 * @return A list if directories sorted from the last in a tree to the first
	 *         one.
	 */
	private List<File> selectDirectories(Collection<File> filesAndDirs) {
		final List<File> directories = new ArrayList<File>();
		for (final File file : filesAndDirs)
			if (file.isDirectory())
				directories.add(file);
		Collections.sort(directories);
		Collections.reverse(directories);
		return directories;
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#deleteDebianPackages(java.util.Collection)
	 */
	public boolean deleteDebianPackages(Collection<Package> deleteList) {
		return delegate.deleteDebianPackages(deleteList);
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#deleteOldPackages(java.util.Collection)
	 */
	public boolean deleteOldPackages(Collection<Package> deleteList)
			throws PackageManagerException {
		boolean ret = false;
		try {
			if (MBeanServerFactory.findMBeanServer(null).get(0).isRegistered(
					new ObjectName("tcat:service=NFSService")))
				ret = doNFSremove(deleteList);
			else if (delegate.deleteOldPackages(deleteList))
				if (delegate.removePackagesFromRemovedDB(new ArrayList<Package>(
						deleteList))) {
					ret = true;
					delegate.setActprogress(new Double(delegate.getMaxProgress())
							.intValue());
					delegate.setIsDoneTrue();

				} else
					throw new PackageManagerException(
							PreferenceStoreHolder
									.getPreferenceStoreByName("Screen")
									.getPreferenceAsString(
											"PackageManagerBean.doNFSremove.removedDB",
											"No entry found for PackageManagerBean.doNFSremove.removedDB"));
		} catch (final MalformedObjectNameException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		} catch (final NullPointerException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		}
		return ret;

	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#findConflicts(java.util.List)
	 */
	public String findConflicts(List<Package> packList) {
		return delegate.findConflicts(packList);
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#getAlreadyDeletedPackages()
	 */
	public Collection<Package> getAlreadyDeletedPackages() {
		return delegate.getAlreadyDeletedPackages();
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#getDebianFilePackages()
	 */
	public Collection<Package> getDebianFilePackages() {
		return delegate.getDebianFilePackages();
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#getFreeDiskSpace()
	 */
	public long getFreeDiskSpace() throws PackageManagerException {
		return delegate.getFreeDiskSpace();
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#getInstallablePackages()
	 */
	public Collection<Package> getInstallablePackages()
			throws PackageManagerException {
		return delegate.getInstallablePackages();
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#getInstalledPackages()
	 */
	public Collection<Package> getInstalledPackages() {
		return delegate.getInstalledPackages();
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#getUpdateablePackages()
	 */
	public Collection<Package> getUpdateablePackages() {
		return delegate.getUpdateablePackages();
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#install(java.util.Collection)
	 */
	public boolean install(Collection<Package> installList)
			throws PackageManagerException {
		return delegate.install(installList);
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#isDependencyOf(java.util.Collection)
	 */
	public List<Package> isDependencyOf(Collection<Package> packList) {
		return delegate.isDependencyOf(packList);
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#solveDependencies(java.util.Collection)
	 */
	public List<Package> solveDependencies(Collection<Package> installList) {
		return delegate.solveDependencies(installList);
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#update(java.util.Collection)
	 */
	public boolean update(Collection<Package> oldPacks)
			throws PackageManagerException {
		boolean ret = false;
		try {
			if (MBeanServerFactory.findMBeanServer(null).get(0).isRegistered(
					new ObjectName("tcat:service=NFSService"))) {
				if (startDelete(oldPacks)) {
					if (delegate.update(oldPacks))
						ret = true;
					else
						throw new PackageManagerException(PreferenceStoreHolder
								.getPreferenceStoreByName("screen").getPreferenceAsString(
										"preferenceKey", "defaultValue"));
				} else
					throw new PackageManagerException(PreferenceStoreHolder
							.getPreferenceStoreByName("screen").getPreferenceAsString(
									"preferenceKey", "defaultValue"));
			} else if (delegate.delete(oldPacks)) {
				if (delegate.update(oldPacks))
					ret = true;
				else
					throw new PackageManagerException(PreferenceStoreHolder
							.getPreferenceStoreByName("screen").getPreferenceAsString(
									"preferenceKey", "defaultValue"));
			} else
				throw new PackageManagerException(PreferenceStoreHolder
						.getPreferenceStoreByName("screen").getPreferenceAsString(
								"preferenceKey", "defaultValue"));

		} catch (final IOException e) {
			e.printStackTrace();
			logger.error(e.toString());
			throw new PackageManagerException(e.toString());
		} catch (final MalformedObjectNameException e) {
			e.printStackTrace();
			throw new PackageManagerException(e.toString());
		} catch (final NullPointerException e) {
			e.printStackTrace();
			throw new PackageManagerException(e.toString());
		}
		delegate.setIsDoneTrue();
		return ret;
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#getChangelogFile(org.openthinclient.util.dpkg.Package)
	 */
	public Collection<String> getChangelogFile(Package p) throws IOException {
		return delegate.getChangelogFile(p);
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#removeConflicts()
	 */
	public boolean removeConflicts() {
		return delegate.removeConflicts();
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#solveConflicts(java.util.Collection)
	 */
	public Collection<Package> solveConflicts(Collection<Package> selectedList) {
		return delegate.solveConflicts(selectedList);
	}

	/**
	 * 
	 * @param fromToMap a map out of Sets with the old Files on the one hand an
	 *          the new File locations on the other
	 * @return true if all file could be moved from to file location (here only
	 *         the move in the NFS DB is Made!)
	 * @throws PackageManagerException
	 */
	private boolean doNFSmove(HashMap<File, File> fromToMap)
			throws PackageManagerException {
		ObjectName objectName = null;
		try {
			objectName = new ObjectName("tcat:service=NFSService");
		} catch (final MalformedObjectNameException e1) {
			e1.printStackTrace();
			throw new PackageManagerException(e1);
		} catch (final NullPointerException e1) {
			e1.printStackTrace();
			throw new PackageManagerException(e1);
		}
		final MBeanServer server = MBeanServerFactory.findMBeanServer(null).get(0);

		try {
			if (Boolean.FALSE.equals(server.invoke(objectName, "moveMoreFiles",
					new Object[]{delegate.getFromToFileMap()},
					new String[]{"java.util.HashMap"}))) {
				final HashMap<File, File> backmap = new HashMap<File, File>();
				for (final Map.Entry entry : delegate.getFromToFileMap().entrySet())
					backmap.put((File) entry.getValue(), (File) entry.getKey());
				if (Boolean.FALSE.equals(server.invoke(objectName, "moveMoreFiles",
						new Object[]{backmap}, new String[]{"java.util.HashMap"}))) {
					final StringBuffer sb = new StringBuffer();
					for (final Map.Entry<File, File> en : delegate.getFromToFileMap()
							.entrySet())
						sb.append(en.getKey().getPath() + " -> " + en.getValue().getPath());
					throw new PackageManagerException(PreferenceStoreHolder
							.getPreferenceStoreByName("Screen").getPreferenceAsString(
									"PackageManagerBean.doNFSmove.fatalError",
									"No entry found for PackageManagerBean.doNFSmove.fatalError")
							+ " \n" + sb.toString());
				} else
					throw new PackageManagerException(
							PreferenceStoreHolder
									.getPreferenceStoreByName("Screen")
									.getPreferenceAsString(
											"PackageManagerBean.doNFSmove.couldNotMove",
											"No entry found for PackageManagerBean.doNFSmove.couldNotMove"));
			} else if (!callDeleteNFS(delegate.getRemoveDirectoryList())) {
				final StringBuffer sb = new StringBuffer();
				for (final File fi : delegate.getRemoveDirectoryList())
					sb.append(fi.getPath());
				throw new PackageManagerException(
						PreferenceStoreHolder
								.getPreferenceStoreByName("Screen")
								.getPreferenceAsString(
										"PackageManagerBean.doNFSmove.couldNotRemove",
										"No entry found for PackageManagerBean.doNFSmove.couldNotRemove")
								+ " \n" + sb.toString());
			}
			return true;
		} catch (final InstanceNotFoundException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		} catch (final MBeanException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		} catch (final ReflectionException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		}
	}

	/**
	 * 
	 * @param packList list of packages which REALLY should be removed
	 * @return
	 * @throws PackageManagerException
	 */
	private boolean doNFSremove(Collection<Package> packList)
			throws PackageManagerException {

		final List<File> fileList = new ArrayList<File>();
		for (final Package pkg : packList) {
			fileList.addAll(delegate.getRemoveDBFileList(pkg.getName()));
			fileList.addAll(delegate.getRemoveDBDirList(pkg.getName()));
		}
		Collections.sort(fileList);
		Collections.reverse(fileList);
		delegate.setActprogress(new Double(delegate.getMaxProgress() * 0.1)
				.intValue());
		if (callDeleteNFS(fileList)) {
			delegate.setActprogress(new Double(delegate.getMaxProgress() * 0.8)
					.intValue());
			if (delegate
					.removePackagesFromRemovedDB(new ArrayList<Package>(packList))) {
				delegate.setActprogress(new Double(delegate.getMaxProgress())
						.intValue());
				delegate.setIsDoneTrue();
				return true;
			}
			throw new PackageManagerException(PreferenceStoreHolder
					.getPreferenceStoreByName("Screen").getPreferenceAsString(
							"PackageManagerBean.doNFSremove.NFSProblem",
							"No entry found for PackageManagerBean.doNFSremove.NFSProblem")
					+ " \n" + fileList.toString());
		} else {
			final StringBuffer sb = new StringBuffer();
			for (final File fi : fileList)
				sb.append(fi.getPath());
			throw new PackageManagerException(PreferenceStoreHolder
					.getPreferenceStoreByName("Screen").getPreferenceAsString(
							"PackageManagerBean.doNFSremove.NFSProblem",
							"No entry found for PackageManagerBean.doNFSremove.NFSProblem")
					+ " \n" + sb.toString());
		}

	}

	/**
	 * @param fileList
	 * @return TRUE only if all the given files and also their NFS handels could
	 *         be removed correctly otherwise FALSE
	 * @throws PackageManagerException
	 */
	private boolean callDeleteNFS(List<File> fileList)
			throws PackageManagerException {
		ObjectName objectName = null;
		try {
			objectName = new ObjectName("tcat:service=NFSService");
		} catch (final MalformedObjectNameException e1) {
			e1.printStackTrace();
			throw new PackageManagerException(e1);
		} catch (final NullPointerException e1) {
			e1.printStackTrace();
			throw new PackageManagerException(e1);
		}
		final MBeanServer server = MBeanServerFactory.findMBeanServer(null).get(0);
		try {
			if (Boolean.FALSE.equals(server.invoke(objectName, "removeFilesFromNFS",
					new Object[]{fileList}, new String[]{"java.util.List"})))
				throw new PackageManagerException(PreferenceStoreHolder
						.getPreferenceStoreByName("Screen").getPreferenceAsString(
								"PackageManagerBean.doNFSremove.NFSProblem",
								"No entry found for PackageManagerBean.doNFSremove.NFSProblem"));
			else
				return true;

		} catch (final InstanceNotFoundException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		} catch (final MBeanException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		} catch (final ReflectionException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		}
		// return false;
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#checkIfPackageMangerIsIn(java.util.Collection)
	 */
	public Collection<Package> checkIfPackageMangerIsIn(
			Collection<Package> deleteList) {
		return delegate.checkIfPackageMangerIsIn(deleteList);
	}

	// private void doServices(startStop doThis) throws InstanceNotFoundException,
	// MBeanException, ReflectionException, PackageManagerException {
	// ObjectName objectName = null;
	// String operation = "";
	// switch (doThis){
	// case START :
	// operation = "start";
	// case STOP :
	// operation = "stop";
	//
	// }
	// for (int i = 0; i < 4; i++) {
	// String service = "";
	// switch (i){
	// case 0 :
	// service = "NFSService";
	// case 1 :
	// service = "ConfigService";
	// case 2 :
	// service = "SyslogService";
	// case 3 :
	// service = "TFTPService";
	// }
	// try {
	// objectName = new ObjectName("tcat:service=" + service);
	// } catch (MalformedObjectNameException e1) {
	// throw new PackageManagerException(e1);
	// } catch (NullPointerException e1) {
	// throw new PackageManagerException(e1);
	// }
	// MBeanServer server = (MBeanServer) MBeanServerFactory.findMBeanServer(
	// null).get(0);
	//
	// server.invoke(objectName, operation, new Object[]{}, new String[]{});
	//
	// }
	// }

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#getActprogress()
	 */
	public int getActprogress() {
		return delegate.getActprogress();
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#isDone()
	 */
	public boolean isDone() {
		return delegate.isDone();
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#setActprogress(int)
	 */
	public void setActprogress(int actprogress) {
		delegate.setActprogress(actprogress);
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#refreshIsDone()
	 */
	public void refreshIsDone() {
		delegate.refreshIsDone();
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#getMaxProgress()
	 */
	public int getMaxProgress() {
		return delegate.getMaxProgress();
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#getActMaxFileSize()
	 */
	public int[] getActMaxFileSize() {
		return delegate.getActMaxFileSize();
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#getActPackName()
	 */
	public String getActPackName() {
		return delegate.getActPackName();
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#resetValuesForDisplaying()
	 */
	public void resetValuesForDisplaying() {
		delegate.resetValuesForDisplaying();

	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#refreshSolveDependencies()
	 */
	public void refreshSolveDependencies() {
		delegate.refreshSolveDependencies();

	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#updateCacheDB()
	 */
	public boolean updateCacheDB() throws PackageManagerException {
		return delegate.updateCacheDB();
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#setIsDoneTrue()
	 */
	public void setIsDoneTrue() {
		delegate.setIsDoneTrue();

	}

	public boolean addWarning(String warning) {
		return delegate.addWarning(warning);
	}

	public List<String> getWarnings() {
		return delegate.getWarnings();
	}
}

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
package org.openthinclient.console.nodes.pkgmgr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.openide.ErrorManager;
import org.openthinclient.console.Messages;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.util.dpkg.Package;

/**
 * This class implements all methods of the PackageManager it cashes the most
 * important values, so that it is not neccessary to connect to the EJB for each
 * simple Package list which is never changed since the package manager is
 * started so we can do most things more efficiently. Cause we don't need to
 * connect to the server only while updating the GUI.
 * 
 * It describes an interface to the Package Managers EJB.
 * 
 * it is the only class in the whole console which can do a direct connection to
 * the PackageMAnager/DPKGPackageManager through the EJB.
 * 
 * @author tauschfn
 * 
 */
public class PackageManagerDelegation implements PackageManager {

	private static PackageManager pkgmgr;
	private static List<Package> installedPackages;
	private static List<Package> installablePackages;
	private static List<Package> updateablePackages;
	private static List<Package> removedPackages;
	private static List<Package> debianPackages;
	private static long freeDiskSpace;
	private static HashMap<Package, List<String>> changelog;
	private List<String> warnings;

	/**
	 * @param Properties with which there could be started a connection to the
	 *          PackageManagerBean
	 */
	public PackageManagerDelegation(Properties p) {
		try {
			pkgmgr = (PackageManager) new InitialContext(p)
					.lookup("PackageManagerBean/remote");
			installablePackages = new ArrayList<Package>(pkgmgr
					.getInstallablePackages());
			installedPackages = new ArrayList<Package>(pkgmgr.getInstalledPackages());
			updateablePackages = new ArrayList<Package>(pkgmgr
					.getUpdateablePackages());
			removedPackages = new ArrayList<Package>(pkgmgr
					.getAlreadyDeletedPackages());
			debianPackages = new ArrayList<Package>(pkgmgr.getDebianFilePackages());
			changelog = new HashMap<Package, List<String>>();
		} catch (final NamingException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		} catch (final PackageManagerException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}

	}

	/**
	 * @return PackageManagerDelegation
	 */
	public PackageManagerDelegation getPackageManagerDelegation() {
		return this;
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#checkForAlreadyInstalled(java.util.List)
	 */
	public String checkForAlreadyInstalled(List<Package> installList) {
		return pkgmgr.checkForAlreadyInstalled(installList);
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#checkIfPackageMangerIsIn(java.util.Collection)
	 */
	public Collection<Package> checkIfPackageMangerIsIn(
			Collection<Package> deleteList) {
		Package packageManager = null;
		for (final Package pkg : deleteList)
			if (pkg.isPackageManager())
				packageManager = pkg;
		if (packageManager != null)
			deleteList.remove(packageManager);
		return deleteList;
	}

	public void close() throws PackageManagerException {
		pkgmgr.close();

	}

	public boolean delete(Collection<Package> collection) throws IOException,
			PackageManagerException {
		if (pkgmgr.delete(collection)) {
			removedPackages = new ArrayList<Package>(pkgmgr
					.getAlreadyDeletedPackages());
			installablePackages = new ArrayList<Package>(pkgmgr
					.getInstallablePackages());
			installedPackages = new ArrayList<Package>(pkgmgr.getInstalledPackages());
			return true;
		}
		return false;
	}

	public boolean deleteDebianPackages(Collection<Package> collection) {
		if (pkgmgr.deleteDebianPackages(collection)) {
			debianPackages = new ArrayList<Package>(pkgmgr.getDebianFilePackages());
			return true;
		}
		return false;
	}

	public boolean deleteOldPackages(Collection<Package> collection)
			throws PackageManagerException {
		if (pkgmgr.deleteOldPackages(collection)) {
			removedPackages = new ArrayList<Package>(pkgmgr
					.getAlreadyDeletedPackages());
			return true;
		}
		return false;
	}

	public String findConflicts(List<Package> list) {
		return pkgmgr.findConflicts(list);
	}

	public int[] getActMaxFileSize() {
		return pkgmgr.getActMaxFileSize();
	}

	public String getActPackName() {
		return pkgmgr.getActPackName();
	}

	public int getActprogress() {
		return pkgmgr.getActprogress();
	}

	public Collection<Package> getAlreadyDeletedPackages() {
		return removedPackages;
	}

	public Collection<String> getChangelogFile(Package package1)
			throws IOException {
		if (changelog != null && changelog.containsKey(package1))
			return changelog.get(package1);
		List<String> tmp;
		tmp = new ArrayList<String>(pkgmgr.getChangelogFile(package1));
		if (null == tmp || tmp == Collections.EMPTY_LIST || tmp.size() == 0)
			tmp
					.add(Messages
							.getString("node.PackageNode.PackageDetailView.createChangelogPanel.noChangeLogFile"));
		changelog.put(package1, tmp);
		return tmp;
	}

	public Collection<Package> getDebianFilePackages() {
		return debianPackages;
	}

	public long getFreeDiskSpace() throws PackageManagerException {
		if (freeDiskSpace == 0)
			freeDiskSpace = pkgmgr.getFreeDiskSpace();
		return freeDiskSpace;
	}

	public Collection<Package> getInstallablePackages()
			throws PackageManagerException {
		return installablePackages;
	}

	public Collection<Package> getInstalledPackages() {
		return installedPackages;
	}

	public int getMaxProgress() {
		return pkgmgr.getMaxProgress();
	}

	public Collection<Package> getUpdateablePackages() {
		return updateablePackages;
	}

	public boolean install(Collection<Package> collection)
			throws PackageManagerException {
		if (pkgmgr.install(collection)) {
			installablePackages = new ArrayList<Package>(pkgmgr
					.getInstallablePackages());
			installedPackages = new ArrayList<Package>(pkgmgr.getInstalledPackages());
			debianPackages = new ArrayList<Package>(pkgmgr.getDebianFilePackages());
			return true;
		}
		return false;
	}

	public List<Package> isDependencyOf(Collection<Package> collection) {
		return pkgmgr.isDependencyOf(collection);
	}

	public boolean isDone() {
		return pkgmgr.isDone();
	}

	public void refreshIsDone() {
		pkgmgr.refreshIsDone();

	}

	public void refreshSolveDependencies() {
		pkgmgr.refreshSolveDependencies();

	}

	public boolean removeConflicts() {
		return pkgmgr.removeConflicts();
	}

	public void resetValuesForDisplaying() {
		pkgmgr.resetValuesForDisplaying();

	}

	public void setActprogress(int actprogress) {
		pkgmgr.setActprogress(actprogress);

	}

	public Collection<Package> solveConflicts(Collection<Package> selectedList) {
		return pkgmgr.solveConflicts(selectedList);
	}

	public List<Package> solveDependencies(Collection<Package> collection) {
		return pkgmgr.solveDependencies(collection);
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#update(java.util.Collection)
	 */
	public boolean update(Collection<Package> collection)
			throws PackageManagerException {
		if (pkgmgr.update(collection)) {
			setIsDoneTrue();
			installedPackages = new ArrayList<Package>(pkgmgr.getInstalledPackages());
			updateablePackages = new ArrayList<Package>(pkgmgr
					.getUpdateablePackages());
			debianPackages = new ArrayList<Package>(pkgmgr.getDebianFilePackages());
			removedPackages = new ArrayList<Package>(pkgmgr
					.getAlreadyDeletedPackages());
			return true;
		}
		return false;
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#updateCacheDB()
	 */
	public boolean updateCacheDB() throws PackageManagerException {
		boolean success = false;
		if (pkgmgr.updateCacheDB()) {
			installablePackages = new ArrayList<Package>(pkgmgr
					.getInstallablePackages());
			updateablePackages = new ArrayList<Package>(pkgmgr
					.getUpdateablePackages());
			success = true;
		}
		final List<String> warningsList = pkgmgr.getWarnings();
		if (warningsList.size() != 0)
			for (final String warning : warningsList)
				ErrorManager.getDefault().notify(new Throwable(warning));
		return success;
	}

	/**
	 * Refreshes the
	 * 
	 * @param what describes which list should be refresh also, possible that all
	 *          lists are refreshed
	 * @throws PackageManagerException
	 */
	public void refresh(int what) throws PackageManagerException {
		setActprogress(new Double(getMaxProgress() * 0.95).intValue());
		switch (what){
			case 0 :
				installedPackages = new ArrayList<Package>(pkgmgr
						.getInstalledPackages());
				updateablePackages = new ArrayList<Package>(pkgmgr
						.getUpdateablePackages());
				installablePackages = new ArrayList<Package>(pkgmgr
						.getInstallablePackages());
				removedPackages = new ArrayList<Package>(pkgmgr
						.getAlreadyDeletedPackages());
				debianPackages = new ArrayList<Package>(pkgmgr.getDebianFilePackages());
				break;
			case 1 :
				installedPackages = new ArrayList<Package>(pkgmgr
						.getInstalledPackages());
				break;
			case 2 :
				installablePackages = new ArrayList<Package>(pkgmgr
						.getInstallablePackages());
				break;
			case 3 :
				updateablePackages = new ArrayList<Package>(pkgmgr
						.getUpdateablePackages());
				break;
			case 4 :
				removedPackages = new ArrayList<Package>(pkgmgr
						.getAlreadyDeletedPackages());
				break;
			case 5 :
				debianPackages = new ArrayList<Package>(pkgmgr.getDebianFilePackages());
				break;

		}
		setActprogress(getMaxProgress());
		setIsDoneTrue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#setIsDoneTrue()
	 */
	public void setIsDoneTrue() {
		pkgmgr.setIsDoneTrue();
	}

	public boolean addWarning(String warning) {
		return pkgmgr.addWarning(warning);
	}

	public List<String> getWarnings() {
		return pkgmgr.getWarnings();
	}
}

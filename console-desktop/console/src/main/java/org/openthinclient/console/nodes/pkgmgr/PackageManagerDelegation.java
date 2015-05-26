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
import javax.swing.SwingUtilities;

import org.openide.ErrorManager;
import org.openthinclient.console.Messages;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerTaskSummary;
import org.openthinclient.util.dpkg.Package;

/**
 * This class implements all methods of the PackageManager it cashes the most
 * important values, so that it is not necessary to connect to the EJB for each
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

	private PackageManager pkgmgr;
	private List<Package> installedPackages;
	private List<Package> installablePackages;
	private List<Package> updateablePackages;
	private List<Package> removedPackages;
	private List<Package> debianPackages;
	private long freeDiskSpace;
	private HashMap<Package, List<String>> changelog;

	// private List<String> warnings;

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
		final String ret = pkgmgr.checkForAlreadyInstalled(installList);
		checkForWarnings();
		return ret;
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
		checkForWarnings();
		return deleteList;
	}

	public void close()
	// throws PackageManagerException
	{
		try {
			pkgmgr.close();
		} catch (final PackageManagerException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}
		checkForWarnings();
	}

	public boolean delete(Collection<Package> collection)
	// throws IOException
	// , PackageManagerException
	{

		try {
			if (pkgmgr.delete(collection)) {
				removedPackages = new ArrayList<Package>(pkgmgr
						.getAlreadyDeletedPackages());
				installablePackages = new ArrayList<Package>(pkgmgr
						.getInstallablePackages());
				installedPackages = new ArrayList<Package>(pkgmgr
						.getInstalledPackages());
				setNewFreeDiskSpace();
				checkForWarnings();
				return true;
			}
		} catch (final PackageManagerException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		} catch (final IOException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}
		return false;
	}

	public boolean deleteDebianPackages(Collection<Package> collection) {
		if (pkgmgr.deleteDebianPackages(collection)) {
			debianPackages = new ArrayList<Package>(pkgmgr.getDebianFilePackages());
			checkForWarnings();
			setNewFreeDiskSpace();
			return true;
		}
		checkForWarnings();
		return false;
	}

	public boolean deleteOldPackages(Collection<Package> collection)
	// throws PackageManagerException
	{
		try {
			if (pkgmgr.deleteOldPackages(collection)) {
				removedPackages = new ArrayList<Package>(pkgmgr
						.getAlreadyDeletedPackages());
				checkForWarnings();
				setNewFreeDiskSpace();
				return true;
			}
		} catch (final PackageManagerException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}
		checkForWarnings();
		return false;
	}

	public String findConflicts(List<Package> list) {
		final String ret = pkgmgr.findConflicts(list);
		checkForWarnings();
		return ret;
	}

	public int[] getActMaxFileSize() {
		final int[] ret = pkgmgr.getActMaxFileSize();
		checkForWarnings();
		return ret;
	}

	public String getActPackName() {
		final String ret = pkgmgr.getActPackName();
		checkForWarnings();
		return ret;
	}

	public int getActprogress() {
		final int ret = pkgmgr.getActprogress();
//		checkForWarnings();
		return ret;
	}

	public Collection<Package> getAlreadyDeletedPackages() {
		return removedPackages;
	}

	public Collection<String> getChangelogFile(Package package1)
	// throws IOException
	{
		if (changelog != null && changelog.containsKey(package1))
			return changelog.get(package1);
		List<String> tmp;
		try {
			tmp = new ArrayList<String>(pkgmgr.getChangelogFile(package1));

			if (null == tmp || tmp == Collections.EMPTY_LIST || tmp.size() == 0)
				tmp
						.add(Messages
								.getString("node.PackageNode.PackageDetailView.createChangelogPanel.noChangeLogFile"));
			changelog.put(package1, tmp);
			checkForWarnings();

			return tmp;
		} catch (final IOException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}
		return Collections.EMPTY_LIST;
	}

	public Collection<Package> getDebianFilePackages() {
		return debianPackages;
	}

	public long getFreeDiskSpace()
	// throws PackageManagerException
	{
		if (freeDiskSpace == 0)
			try {
				freeDiskSpace = pkgmgr.getFreeDiskSpace();
			} catch (final PackageManagerException e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
			}
		checkForWarnings();
		return freeDiskSpace;
	}

	private void setNewFreeDiskSpace() {
		try {
			freeDiskSpace = pkgmgr.getFreeDiskSpace();
		} catch (final PackageManagerException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}
		checkForWarnings();
	}

	public Collection<Package> getInstallablePackages()
			throws PackageManagerException {
		return installablePackages;
	}

	public Collection<Package> getInstalledPackages() {
		return installedPackages;
	}

	public int getMaxProgress() {
		final int ret = pkgmgr.getMaxProgress();
//		checkForWarnings();
		return ret;
	}

	public Collection<Package> getUpdateablePackages() {
		return updateablePackages;
	}

	public boolean install(Collection<Package> collection)
	// throws PackageManagerException
	{
		try {
			if (pkgmgr.install(collection)) {
				installablePackages = new ArrayList<Package>(pkgmgr
						.getInstallablePackages());
				installedPackages = new ArrayList<Package>(pkgmgr
						.getInstalledPackages());
				debianPackages = new ArrayList<Package>(pkgmgr.getDebianFilePackages());
				setNewFreeDiskSpace();
				checkForWarnings();
				return true;
			}
		} catch (final PackageManagerException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}
		checkForWarnings();
		return false;
	}

	public List<Package> isDependencyOf(Collection<Package> collection) {
		final List<Package> ret = pkgmgr.isDependencyOf(collection);
		checkForWarnings();
		return ret;
	}

	public boolean isDone() {
		final boolean ret = pkgmgr.isDone();
//		checkForWarnings();
		return ret;
	}

	public void refreshIsDone() {
		pkgmgr.refreshIsDone();
		checkForWarnings();
	}

	public void refreshSolveDependencies() {
		pkgmgr.refreshSolveDependencies();
		checkForWarnings();
	}

	public boolean removeConflicts() {
		final boolean ret = pkgmgr.removeConflicts();
		checkForWarnings();
		return ret;
	}

	public void resetValuesForDisplaying() {
		pkgmgr.resetValuesForDisplaying();
//		checkForWarnings();
	}

	public void setActprogress(int actprogress) {
		pkgmgr.setActprogress(actprogress);
		checkForWarnings();
	}

	public Collection<Package> solveConflicts(Collection<Package> selectedList) {
		final Collection<Package> ret = pkgmgr.solveConflicts(selectedList);
		checkForWarnings();
		return ret;
	}

	public List<Package> solveDependencies(Collection<Package> collection) {
		final List<Package> ret = pkgmgr.solveDependencies(collection);
		checkForWarnings();
		return ret;
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#update(java.util.Collection)
	 */
	public boolean update(Collection<Package> collection)
	// throws PackageManagerException
	{
		try {
			if (pkgmgr.update(collection)) {
				setIsDoneTrue();
				installedPackages = new ArrayList<Package>(pkgmgr
						.getInstalledPackages());
				updateablePackages = new ArrayList<Package>(pkgmgr
						.getUpdateablePackages());
				debianPackages = new ArrayList<Package>(pkgmgr.getDebianFilePackages());
				removedPackages = new ArrayList<Package>(pkgmgr
						.getAlreadyDeletedPackages());
				checkForWarnings();
				return true;
			}
		} catch (final PackageManagerException e) {
			ErrorManager.getDefault().notify(e);
			e.printStackTrace();
		}
		checkForWarnings();
		return false;
	}

	/*
	 * @see org.openthinclient.pkgmgr.PackageManager#updateCacheDB()
	 */
	public boolean updateCacheDB() throws PackageManagerException {
		// boolean success = false;
		// try {
		if (pkgmgr.updateCacheDB()) {
			installablePackages = new ArrayList<Package>(pkgmgr
					.getInstallablePackages());
			updateablePackages = new ArrayList<Package>(pkgmgr
					.getUpdateablePackages());
			// success = true;
			// not executing checkForWarnings here, as it will be handled by the caller.
//			checkForWarnings();
			return true;
		}
		// } catch (PackageManagerException e) {
		// e.printStackTrace();
		// ErrorManager.getDefault().notify(e);
		// }
		return false;

	}

	/**
	 * Refreshes the
	 * 
	 * @param what describes which list should be refresh also, possible that all
	 *          lists are refreshed
	 * @throws PackageManagerException
	 */
	public void refresh(int what)
	// throws PackageManagerException
	{
		setActprogress(new Double(getMaxProgress() * 0.95).intValue());
		try {
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
					debianPackages = new ArrayList<Package>(pkgmgr
							.getDebianFilePackages());
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
					debianPackages = new ArrayList<Package>(pkgmgr
							.getDebianFilePackages());
					break;

			}
		} catch (final PackageManagerException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}
		setActprogress(getMaxProgress());
		setIsDoneTrue();
		checkForWarnings();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#setIsDoneTrue()
	 */
	public void setIsDoneTrue() {
		pkgmgr.setIsDoneTrue();
		checkForWarnings();
	}

	public boolean addWarning(String warning) {
		final boolean ret = pkgmgr.addWarning(warning);
		checkForWarnings();
		return ret;
	}

	public PackageManagerTaskSummary fetchTaskSummary() {
		// actually this method doesn't make much sense. It has been
		// reimplemented that way, to keep the logic identical to the previous
		// getWarnings().
		PackageManagerTaskSummary taskSummary = pkgmgr.fetchTaskSummary();
		// there is almost no chance that there will be a warning, was calling
		// getTaskSummary will reset the server state.
		checkForWarnings();
		return taskSummary;
	}
	
	private void checkForWarnings() {
		final PackageManagerTaskSummary taskSummary = pkgmgr.fetchTaskSummary();
		if (taskSummary != null && taskSummary.getWarnings() != null
				&& taskSummary.getWarnings().size() != 0)
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					PackageManagerJobSummaryDialogDescriptor.show(
							"Package Management", taskSummary.getWarnings());
				}
			});

	}

}

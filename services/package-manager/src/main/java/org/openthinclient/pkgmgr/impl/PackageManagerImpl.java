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
package org.openthinclient.pkgmgr.impl;

import org.openthinclient.pkgmgr.I18N;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerTaskSummary;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.service.nfs.NFS;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
 * @author jn
 * 
 */
public class PackageManagerImpl implements PackageManager {

	private static final Logger logger = LoggerFactory.getLogger(PackageManagerImpl.class);
	private final DPKGPackageManager delegate;
  private final NFS nfs;

  public PackageManagerImpl(DPKGPackageManager delegate, NFS nfs) {
    if (delegate == null) {
      throw new IllegalArgumentException("delegate must not be null");
    }
    this.delegate = delegate;
    // not doing any null checking, as it should be possible to work with the package manager, even if the NFS service is not available.
    this.nfs = nfs;
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
    boolean ret = false;
    if (nfs != null)
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
      } else {
        final String message = I18N.getMessage("PackageManagerBean.doNFSremove.removedDB");
        logger.warn(message);
        addWarning(message);
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
					final String message = I18N.getMessage("PackageManagerBean.doNFSremove.NFSProblem")
							+ " \n" + sb.toString();
					logger.warn(message);
					addWarning(message);
					// e.printStackTrace();
					// throw new PackageManagerException(
					// PreferenceStoreHolder
					// .getPreferenceStoreByName("Screen")
					// .getPreferenceAsString(
					// "PackageManagerBean.doNFSremove.NFSProblem",
					// "No entry found for PackageManagerBean.doNFSremove.NFSProblem")
					// + " \n" + sb.toString());
				}
			} else {
				final String message = I18N.getMessage("PackageManagerBean.delete.DBtransfer");
				logger.warn(message);
				addWarning(message);
				// throw new PackageManagerException(PreferenceStoreHolder
				// .getPreferenceStoreByName("Screen").getPreferenceAsString(
				// "PackageManagerBean.delete.DBtransfer",
				// "No entry found for PackageManagerBean.delete.DBtransfer"));

			}
		} else {
			callDeleteNFS(selectDirectories(delegate.getFromToFileMap().values()));
			final String message = I18N.getMessage("PackageManagerBean.delete.doNFSmoveFailed");
			logger.warn(message);
			addWarning(message);
			// throw new PackageManagerException(PreferenceStoreHolder
			// .getPreferenceStoreByName("Screen").getPreferenceAsString(
			// "PackageManagerBean.delete.doNFSmoveFailed",
			// "No entry found for PackageManagerBean.delete.doNFSmoveFailed"));

		}
		return false;
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
    if (nfs != null)
      ret = doNFSremove(deleteList);
    else if (delegate.deleteOldPackages(deleteList))
      if (delegate.removePackagesFromRemovedDB(new ArrayList<Package>(
              deleteList))) {
        ret = true;
        delegate.setActprogress(new Double(delegate.getMaxProgress())
                .intValue());
        delegate.setIsDoneTrue();

      } else {
        final String message = I18N.getMessage("PackageManagerBean.doNFSremove.removedDB");
        logger.warn(message);
        addWarning(message);
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

  @Override
	public boolean update(Collection<Package> oldPacks)
			throws PackageManagerException {
    boolean ret = false;
    try {
      if (nfs != null) {
        if (startDelete(oldPacks)) {
          if (delegate.update(oldPacks))
            ret = true;
          else {
            final String message = I18N.getMessage("PackageManagerBean.update.couldNotDownloadAndInstall");
            logger.warn(message);
            addWarning(message);
          }
          // throw new PackageManagerException(PreferenceStoreHolder
          // .getPreferenceStoreByName("screen").getPreferenceAsString(
          // "preferenceKey", "defaultValue"));
        } else {
          final String message = I18N.getMessage("PackageManagerBean.update.couldNotMoveOldPackages");
          logger.warn(message);
          addWarning(message);
        }
        // throw new PackageManagerException(PreferenceStoreHolder
        // .getPreferenceStoreByName("screen").getPreferenceAsString(
        // "preferenceKey", "defaultValue"));
      } else if (delegate.delete(oldPacks)) {
        if (delegate.update(oldPacks))
          ret = true;
        else {
          final String message = I18N.getMessage("PackageManagerBean.update.couldNotDownloadAndInstall");
          logger.warn(message);
          addWarning(message);
        }
        // throw new PackageManagerException(PreferenceStoreHolder
        // .getPreferenceStoreByName("screen").getPreferenceAsString(
        // "preferenceKey", "defaultValue"));
      } else {
        final String message = I18N.getMessage("PackageManagerBean.update.couldNotMoveOldPackages");
        logger.warn(message);
        addWarning(message);
      }
      // throw new PackageManagerException(PreferenceStoreHolder
      // .getPreferenceStoreByName("screen").getPreferenceAsString(
      // "preferenceKey", "defaultValue"));

    } catch (final IOException e) {
      final String message = I18N.getMessage("PackageManagerBean.update.couldNotMoveOldPackages");
      logger.warn(message, e);
      addWarning(message);
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

    if (!nfs.moveMoreFiles(fromToMap)) {
      final HashMap<File, File> backmap = new HashMap<File, File>();
      for (final Map.Entry entry : fromToMap.entrySet())
        backmap.put((File) entry.getValue(), (File) entry.getKey());
      if (!nfs.moveMoreFiles(backmap)) {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<File, File> en : fromToMap
                .entrySet())
          sb.append(en.getKey().getPath() + " -> " + en.getValue().getPath());
        final String message = I18N.getMessage("PackageManagerBean.doNFSmove.fatalError")
                + " \n" + sb.toString();
        logger.warn(message);
        addWarning(message);
      } else {
        final String message = I18N.getMessage("PackageManagerBean.doNFSmove.couldNotMove");
        logger.warn(message);
        addWarning(message);
      }
    } else if (!callDeleteNFS(delegate.getRemoveDirectoryList())) {
      final StringBuffer sb = new StringBuffer();
      for (final File fi : delegate.getRemoveDirectoryList())
        sb.append(fi.getPath());
      final String message = I18N.getMessage("PackageManagerBean.doNFSmove.couldNotRemove")
              + " \n" + sb.toString();
      logger.warn(message);
      addWarning(message);
    } else {
      return true;
    }
    return false;
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
			final StringBuffer sb = new StringBuffer();
			for (final File fi : fileList)
				sb.append(fi.getPath());
			final String message = I18N.getMessage("PackageManagerBean.doNFSremove.NFSProblem")
					+ " \n" + sb.toString();
			logger.warn(message);
			addWarning(message);
			// return false;
			// e.printStackTrace();
			// throw new PackageManagerException(PreferenceStoreHolder
			// .getPreferenceStoreByName("Screen").getPreferenceAsString(
			// "PackageManagerBean.doNFSremove.NFSProblem",
			// "No entry found for PackageManagerBean.doNFSremove.NFSProblem")
			// + " \n" + fileList.toString());
		} else {
			final StringBuffer sb = new StringBuffer();
			for (final File fi : fileList)
				sb.append(fi.getPath());
			final String message = I18N.getMessage("PackageManagerBean.doNFSremove.NFSProblem")
					+ " \n" + sb.toString();
			logger.warn(message);
			addWarning(message);
			// throw new PackageManagerException(PreferenceStoreHolder
			// .getPreferenceStoreByName("Screen").getPreferenceAsString(
			// "PackageManagerBean.doNFSremove.NFSProblem",
			// "No entry found for PackageManagerBean.doNFSremove.NFSProblem")
			// + " \n" + sb.toString());
		}
		return false;
	}

	/**
	 * @param fileList
	 * @return TRUE only if all the given files and also their NFS handels could
	 *         be removed correctly otherwise FALSE
	 * @throws PackageManagerException
	 */
  private boolean callDeleteNFS(List<File> fileList)
          throws PackageManagerException {
    if (nfs.removeFilesFromNFS(fileList)) {
      final String message = I18N.getMessage("PackageManagerBean.callDeleteNFS.NFSServerConnectionFaild.removeFilesFromNFS");
      logger.warn(message);
      addWarning(message);
      return false;
    }
    return true;
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

	@Override
	public SourceRepository getSourceRepository() {
		return delegate.getSourceRepository();
	}

	public PackageManagerTaskSummary fetchTaskSummary() {
		return delegate.fetchTaskSummary();
	}
	
}

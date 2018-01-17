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

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerTaskSummary;
import org.openthinclient.pkgmgr.SourcesList;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageInstalledContent;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.exception.SourceIntegrityViolationException;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.service.nfs.NFS;
import org.openthinclient.util.dpkg.LocalPackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

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
	private final PackageManager delegate;
  private final NFS nfs;

	public PackageManagerImpl(PackageManager delegate, NFS nfs) {
		if (delegate == null) {
      throw new IllegalArgumentException("delegate must not be null");
    }
    this.delegate = delegate;
    // not doing any null checking, as it should be possible to work with the package manager, even if the NFS service is not available.
    this.nfs = nfs;
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
	 * @see org.openthinclient.pkgmgr.PackageManager#getFreeDiskSpace()
	 */
	public long getFreeDiskSpace() throws PackageManagerException {
		return delegate.getFreeDiskSpace();
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#findByInstalledFalse()
	 */
	public Collection<Package> getInstallablePackages()
			throws PackageManagerException {
		return delegate.getInstallablePackages();
	}

	/*
	 * 
	 * @see org.openthinclient.pkgmgr.PackageManager#findByInstalledTrue()
	 */
	public Collection<Package> getInstalledPackages() {
		return delegate.getInstalledPackages();
	}

	@Override
	public Collection<Package> getInstallablePackagesWithoutInstalledOfSameVersion() {
		return delegate.getInstallablePackagesWithoutInstalledOfSameVersion();
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
	 * @see org.openthinclient.pkgmgr.PackageManager#getChangelogFile(org.openthinclient.pkgmgr.db.Package)
	 */
	public Collection<String> getChangelogFile(Package p) throws IOException {
		return delegate.getChangelogFile(p);
	}

//	/**
//	 *
//	 * @param fromToMap a map out of Sets with the old Files on the one hand an
//	 *          the new File locations on the other
//	 * @return true if all file could be moved from to file location (here only
//	 *         the move in the NFS DB is Made!)
//	 * @throws PackageManagerException
//	 */
	// FIXME MANAGER-102
//	private boolean doNFSmove(HashMap<File, File> fromToMap)
//			throws PackageManagerException {
//
//    if (!nfs.moveMoreFiles(fromToMap)) {
//      final HashMap<File, File> backmap = new HashMap<File, File>();
//      for (final Map.Entry entry : fromToMap.entrySet())
//        backmap.put((File) entry.getValue(), (File) entry.getKey());
//      if (!nfs.moveMoreFiles(backmap)) {
//        final StringBuilder sb = new StringBuilder();
//        for (final Map.Entry<File, File> en : fromToMap
//                .entrySet())
//          sb.append(en.getKey().getPath() + " -> " + en.getValue().getPath());
//        final String message = I18N.getMessage("PackageManagerBean.doNFSmove.fatalError")
//                + " \n" + sb.toString();
//        logger.warn(message);
//        addWarning(message);
//      } else {
//        final String message = I18N.getMessage("PackageManagerBean.doNFSmove.couldNotMove");
//        logger.warn(message);
//        addWarning(message);
//      }
//    } else if (!callDeleteNFS(delegate.getRemoveDirectoryList())) {
//      final StringBuffer sb = new StringBuffer();
//      for (final File fi : delegate.getRemoveDirectoryList())
//        sb.append(fi.getPath());
//      final String message = I18N.getMessage("PackageManagerBean.doNFSmove.couldNotRemove")
//              + " \n" + sb.toString();
//      logger.warn(message);
//      addWarning(message);
//    } else {
//      return true;
//    }
//    return false;
//  }

//	/**
//	 *
//	 * @param packList list of packages which REALLY should be removed
//	 * @return
//	 * @throws PackageManagerException
//	 */
	// FIXME MANAGER-102
//	private boolean doNFSremove(Collection<Package> packList)
//			throws PackageManagerException {
//
//		final List<File> fileList = new ArrayList<File>();
//		for (final Package pkg : packList) {
//			fileList.addAll(delegate.getRemoveDBFileList(pkg.getName()));
//			fileList.addAll(delegate.getRemoveDBDirList(pkg.getName()));
//		}
//		Collections.sort(fileList);
//		Collections.reverse(fileList);
//		delegate.setActprogress(new Double(delegate.getMaxProgress() * 0.1)
//				.intValue());
//		if (callDeleteNFS(fileList)) {
//			delegate.setActprogress(new Double(delegate.getMaxProgress() * 0.8)
//					.intValue());
//			if (delegate
//					.removePackagesFromRemovedDB(new ArrayList<Package>(packList))) {
//				delegate.setActprogress(new Double(delegate.getMaxProgress())
//						.intValue());
//				delegate.setIsDoneTrue();
//				return true;
//			}
//			final StringBuffer sb = new StringBuffer();
//			for (final File fi : fileList)
//				sb.append(fi.getPath());
//			final String message = I18N.getMessage("PackageManagerBean.doNFSremove.NFSProblem")
//					+ " \n" + sb.toString();
//			logger.warn(message);
//			addWarning(message);
//			// return false;
//			// e.printStackTrace();
//			// throw new PackageManagerException(PreferenceStoreHolder
//			// .getPreferenceStoreByName("Screen").getPreferenceAsString(
//			// "PackageManagerBean.doNFSremove.NFSProblem",
//			// "No entry found for PackageManagerBean.doNFSremove.NFSProblem")
//			// + " \n" + fileList.toString());
//		} else {
//			final StringBuffer sb = new StringBuffer();
//			for (final File fi : fileList)
//				sb.append(fi.getPath());
//			final String message = I18N.getMessage("PackageManagerBean.doNFSremove.NFSProblem")
//					+ " \n" + sb.toString();
//			logger.warn(message);
//			addWarning(message);
//			// throw new PackageManagerException(PreferenceStoreHolder
//			// .getPreferenceStoreByName("Screen").getPreferenceAsString(
//			// "PackageManagerBean.doNFSremove.NFSProblem",
//			// "No entry found for PackageManagerBean.doNFSremove.NFSProblem")
//			// + " \n" + sb.toString());
//		}
//		return false;
//	}

//	/**
//	 * @param fileList
//	 * @return TRUE only if all the given files and also their NFS handels could
//	 *         be removed correctly otherwise FALSE
//	 * @throws PackageManagerException
//	 */
	// FIXME MANAGER-102
//  private boolean callDeleteNFS(List<File> fileList)
//          throws PackageManagerException {
//    if (nfs.removeFilesFromNFS(fileList)) {
//      final String message = I18N.getMessage("PackageManagerBean.callDeleteNFS.NFSServerConnectionFaild.removeFilesFromNFS");
//      logger.warn(message);
//      addWarning(message);
//      return false;
//    }
//    return true;
//  }

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
	 * @see org.openthinclient.pkgmgr.PackageManager#updateCacheDB()
	 */
	public ListenableProgressFuture<PackageListUpdateReport> updateCacheDB() {
		return delegate.updateCacheDB();
	}

	public boolean addWarning(String warning) {
		return delegate.addWarning(warning);
	}

//	@Override
//	public SourceRepository getSourceRepository() {
//		return delegate.getSourceRepository();
//	}

	public PackageManagerTaskSummary fetchTaskSummary() {
		return delegate.fetchTaskSummary();
	}

	@Override
	public PackageManagerOperation createOperation() {
		return delegate.createOperation();
	}

	@Override
	public ListenableProgressFuture<PackageManagerOperationReport> execute(PackageManagerOperation operation) {
		return delegate.execute(operation);
	}

	@Override
	public SourcesList getSourcesList() {
		return delegate.getSourcesList();
	}

	@Override
	public LocalPackageRepository getLocalPackageRepository() {
		return delegate.getLocalPackageRepository();
	}

	@Override
	public boolean isInstalled(Package pkg) {
		return delegate.isInstalled(pkg);
	}

	@Override
	public boolean isInstallable(Package pkg) {
		return delegate.isInstallable(pkg);
	}

	@Override
	public PackageManagerConfiguration getConfiguration() {
		return delegate.getConfiguration();
	}

    @Override
    public void deleteSource(Source source) throws SourceIntegrityViolationException {
      delegate.deleteSource(source);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Source saveSource(Source source) {
      return delegate.saveSource(source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Source> findAllSources() {
      return delegate.findAllSources();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSources(List<Source> sources) {
     delegate.saveSources(sources);
    }



   @Override
   public ListenableProgressFuture<PackageListUpdateReport> deleteSourcePackagesFromCacheDB(Source source) {
      return delegate.deleteSourcePackagesFromCacheDB(source);
   }

	@Override
	public List<PackageInstalledContent> getInstalledPackageContents(Package pkg) {
		return delegate.getInstalledPackageContents(pkg);
	}
}

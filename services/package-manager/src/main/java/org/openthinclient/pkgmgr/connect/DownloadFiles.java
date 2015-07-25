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
package org.openthinclient.pkgmgr.connect;

import org.openthinclient.pkgmgr.I18N;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerTaskSummary;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.openthinclient.pkgmgr.I18N;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerTaskSummary;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openthinclient.manager.util.http.DownloadException;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.DownloadManagerFactory;
import org.openthinclient.pkgmgr.I18N;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerTaskSummary;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author tauschfn
 * 
 */
public class DownloadFiles {

	private static final Logger LOG = LoggerFactory.getLogger(DownloadFiles.class);

	private final DPKGPackageManager pkgmgr;

	public DownloadFiles(DPKGPackageManager pkgmgr) {
		this.pkgmgr = pkgmgr;
	}

	public static String byteArrayToHexString(byte[] b) {
		final StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			final int v = b[i] & 0xff;
			if (v < 16)
				sb.append('0');
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase();
	}

	/**
	 * get an ArrayList and starting the download and MD5sum check for the
	 * different files
	 *
	 * @param packages
	 * @throws PackageManagerException
	 * @throws Throwable
	 */
	public boolean downloadAndMD5sumCheck(ArrayList<Package> packages, PackageManagerTaskSummary taskSummary) throws PackageManagerException {

    final File archivesDir = pkgmgr.getConfiguration().getArchivesDir();
    final File partialDir = pkgmgr.getConfiguration().getPartialDir();
		final DownloadManager downloadManager = DownloadManagerFactory.create(pkgmgr.getConfiguration().getProxyConfiguration());

    boolean ret = true;
		for (final Package myPackage : packages) {
			final String packageFileName = myPackage.getFilename();
			final String serverPath = myPackage.getServerPath();
			final FileName.DownloadItem archiveFile = new FileName().getLocationsForDownload(packageFileName, serverPath, archivesDir);
			final FileName.DownloadItem partialFile = new FileName().getLocationsForDownload(packageFileName, serverPath, partialDir);
			final File fileToInstall = partialFile.getLocalFile();
			final File alreadyDownloadedFile = archiveFile.getLocalFile();
			if (alreadyDownloadedFile.isFile()
							&& alreadyDownloadedFile.renameTo(fileToInstall)) {
				if (!checksum(fileToInstall, myPackage))
					ret = false;
			} else
				try {

					downloadManager.downloadTo(new URL(partialFile.getServerPath()), partialFile.getLocalFile());

					// FIXME there is no message, that the validation of the checksum failed!
					if (!checksum(partialFile.getLocalFile(), myPackage))
						return false;
				} catch (final MalformedURLException e) {
					e.printStackTrace();
					String errorMessage = I18N.getMessage("DownloadFiles.downloadAndMD5sumCheck.MalformedURL");
					LOG.error(errorMessage);
					pkgmgr.addWarning(errorMessage);
				} catch (final DownloadException e) {
					String errorMessage = I18N.getMessage("DownloadFiles.downloadAndMD5sumCheck.IOException");
					e.printStackTrace();
					LOG.error(errorMessage);
					pkgmgr.addWarning(errorMessage);
				}
		}
		return ret;
	}

	public boolean checksum(File file, Package myPackage) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			final FileInputStream in = new FileInputStream(file);
			int len;
			final byte[] buf = new byte[4096];

			while ((len = in.read(buf)) > 0)
				md.update(buf, 0, len);
			in.close();

			final byte[] fileMD5sum = md.digest();
			md.reset();

			final String actualMD5Sum = byteArrayToHexString(fileMD5sum);
			final String expectedMD5Sum = myPackage.getMD5sum();
			LOG.info("Verifying MD5 checksum for: " + myPackage.getName()+ ". expected: " + expectedMD5Sum + " actual: " + actualMD5Sum);
			if (expectedMD5Sum.equalsIgnoreCase(actualMD5Sum)) {

				final File targetFile = new File(pkgmgr.getConfiguration().getArchivesDir(), file.getName());

				LOG.info("Package verified. Moving package " + myPackage.getName() + " to " + targetFile.getAbsolutePath());
				if (!file.renameTo(targetFile)) {
					// FIXME we should try it a secound time
          String errorMessage = I18N.getMessage("DownloadFiles.checksum.md5different");
					LOG.error(errorMessage);
					pkgmgr.addWarning(errorMessage);
					return false;
				}
			} else {
				// FIXME first make the checksum a secound time if false delete the
				// file, and redownload it if the checksumtest also false throw a
				// exception like pretty bad some files on the server maybe corrupt...
				String errorMessage = I18N.getMessage("DownloadFiles.checksum.md5different");

				LOG.error(errorMessage);
				pkgmgr.addWarning(errorMessage);
				// uncaughtException(this, new Throwable(PreferenceStoreHolder
				// .getPreferenceStoreByName("Screen").getPreferenceAsString(
				// "checksum.md5different",
				// "Entry not found for checksum.md5different")));
				file.delete();
				return false;
			}
		} catch (final NoSuchAlgorithmException e1) {
			String errorMessage = I18N.getMessage("DownloadFiles.checksum.NoSuchAlgorithmException");
			LOG.error(errorMessage, e1);
			pkgmgr.addWarning(errorMessage);
		} catch (final IOException e) {
			String errorMessage = I18N.getMessage("DownloadFiles.checksum.IOException");
			LOG.error(errorMessage, e);
			pkgmgr.addWarning(errorMessage);
		}
		return true;
	}
}
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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

/**
 * 
 * @author tauschfn
 * 
 */
public class DownloadFiles {

	private static final Logger logger = LoggerFactory.getLogger(DownloadFiles.class);

	private final DPKGPackageManager pkgmgr;

	public DownloadFiles(DPKGPackageManager pkgmgr) {
		this.pkgmgr = pkgmgr;
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

    boolean ret = true;
		for (int i = 0; i < packages.size(); i++) {
			final Package myPackage = packages.get(i);
			final String packageFileName = myPackage.getFilename();
			final String serverPath = myPackage.getServerPath();
			final FileName.DownloadItem archiveFile = new FileName().getLocationsForDownload(
              packageFileName, serverPath, archivesDir);
			final FileName.DownloadItem partialFile = new FileName().getLocationsForDownload(
              packageFileName, serverPath, partialDir);
			final File fileToInstall = partialFile.getLocalFile();
			final File alreadyDownloadedFile = archiveFile.getLocalFile();
			if (alreadyDownloadedFile.isFile()
					&& alreadyDownloadedFile.renameTo(fileToInstall)) {
				if (!checksum(fileToInstall, myPackage))
					ret = false;
			} else
				try {
					final InputStream in = new ConnectToServer(taskSummary)
							.getInputStream(partialFile.getServerPath());
					final FileOutputStream out = new FileOutputStream(partialFile.getLocalFile());
					final int buflength = 4096;
					final double maxsize = pkgmgr.getMaxVolumeinByte();
					final int maxProgress = new Double(pkgmgr.getMaxProgress() * 0.6)
							.intValue();
					final byte[] buf = new byte[buflength];
					int len;
					int anzahl = 0;
					final int beforeStarting = pkgmgr.getActprogress();
					double leneee = 0;
					while ((len = in.read(buf)) > 0) {
						out.write(buf, 0, len);
						anzahl++;
						leneee += len;
						if (anzahl % 25 == 0)
							pkgmgr.setActprogressPlusX((beforeStarting + new Double(leneee
									/ maxsize * maxProgress).intValue()), new Double(
									leneee / 1024).intValue(), new Double(
									myPackage.getSize() / 1024).intValue(), myPackage.getName());
					}
					in.close();
					out.close();
					pkgmgr.setActprogressPlusX((beforeStarting + new Double(leneee
							/ maxsize * maxProgress).intValue()), new Double(leneee / 1024)
							.intValue(), new Double(myPackage.getSize() / 1024).intValue(),
							myPackage.getName());
					if (!checksum(partialFile.getLocalFile(), myPackage))
						ret = false;
				} catch (final MalformedURLException e) {
					e.printStackTrace();
					String errorMessage = I18N.getMessage("DownloadFiles.downloadAndMD5sumCheck.MalformedURL");
					logger.error(errorMessage);
					pkgmgr.addWarning(errorMessage);
				} catch (final IOException e) {
					String errorMessage = I18N.getMessage("DownloadFiles.downloadAndMD5sumCheck.IOException");
					e.printStackTrace();
					logger.error(errorMessage);
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

			if (myPackage.getMD5sum().equalsIgnoreCase(
					byteArrayToHexString(fileMD5sum))) {
				File parentFile = file.getParentFile();
				if (parentFile != null && parentFile.exists())
					parentFile = parentFile.getParentFile();
				final String testFileName = parentFile.getPath() + File.separator
						+ file.getName();
				if (!file.renameTo(new File(testFileName))) {
					// FIXME we should try it a secound time
          String errorMessage = I18N.getMessage("DownloadFiles.checksum.md5different");
					logger.error(errorMessage);
					pkgmgr.addWarning(errorMessage);
					return false;
				}
			} else {
				// FIXME first make the checksum a secound time if false delete the
				// file, and redownload it if the checksumtest also false throw a
				// exception like pretty bad some files on the server maybe corrupt...
				String errorMessage = I18N.getMessage("DownloadFiles.checksum.md5different");

				logger.error(errorMessage);
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
			logger.error(errorMessage, e1);
			pkgmgr.addWarning(errorMessage);
		} catch (final IOException e) {
			String errorMessage = I18N.getMessage("DownloadFiles.checksum.IOException");
			logger.error(errorMessage, e);
			pkgmgr.addWarning(errorMessage);
		}
		return true;
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
}
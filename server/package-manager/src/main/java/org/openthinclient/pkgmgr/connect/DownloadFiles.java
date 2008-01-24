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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;

import com.levigo.util.preferences.PreferenceStoreHolder;

/**
 * 
 * @author tauschfn
 * 
 */
public class DownloadFiles {

	private final DPKGPackageManager pkgmgr;

	public DownloadFiles(DPKGPackageManager pkgmgr) {
		this.pkgmgr = pkgmgr;
	}

	/**
	 * get an ArrayList and starting the download and MD5sum check for the
	 * different files
	 * 
	 * @param packages
	 * @throws Throwable
	 */
	public boolean downloadAndMD5sumCheck(ArrayList<Package> packages) {
		final String archivesDir = PreferenceStoreHolder.getPreferenceStoreByName(
				"tempPackageManager").getPreferenceAsString("archivesDir", null);;
		final String partialDir = PreferenceStoreHolder.getPreferenceStoreByName(
				"tempPackageManager").getPreferenceAsString("partialDir", null);;

		boolean ret = true;

		for (int i = 0; i < packages.size(); i++) {
			final Package myPackage = packages.get(i);
			final String packageFileName = myPackage.getFilename();
			final String serverPath = myPackage.getServerPath();

			final String[] archiveFile = new FileName().getLocationsForDownload(
					packageFileName, serverPath, archivesDir);
			final String[] partialFile = new FileName().getLocationsForDownload(
					packageFileName, serverPath, partialDir);

			final File fileToInstall = new File(partialFile[1]);
			final File alreadyDownloadedFile = new File(archiveFile[1]);

			if (alreadyDownloadedFile.isFile()
					&& alreadyDownloadedFile.renameTo(fileToInstall)) {
				if (!checksum(fileToInstall, myPackage))
					ret = false;
			} else
				try {
					final URL url = new URL(partialFile[0]);
					final InputStream in = url.openStream();
					final FileOutputStream out = new FileOutputStream(partialFile[1]);
					final int buflength = 4096;
					final double maxsize = pkgmgr.getMaxVolumeinByte();
					// 60% download (40% install)
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

					if (!checksum(new File(partialFile[1]), myPackage))
						ret = false;
				} catch (final MalformedURLException e) {
					e.printStackTrace();
				} catch (final IOException e) {
					e.printStackTrace();
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
					// FIXME: throw
					// uncaughtException(this, new Throwable(PreferenceStoreHolder
					// .getPreferenceStoreByName("Screen").getPreferenceAsString(
					// "checksum.moveFile", "Entry not found for checksum.moveFile")
					// + files.get(0).getPath()));
					;
					return false;
				}
			} else {
				// uncaughtException(this, new Throwable(PreferenceStoreHolder
				// .getPreferenceStoreByName("Screen").getPreferenceAsString(
				// "checksum.md5different",
				// "Entry not found for checksum.md5different")));
				file.delete();
				return false;
			}
		} catch (final NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
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
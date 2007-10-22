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
 *******************************************************************************/
package org.openthinclient.pkgmgr.connect;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;

import com.levigo.util.preferences.PreferenceStoreHolder;

/**
 * 
 * @author tauschfn
 * 
 */
public class DownloadFiles {

	/**
	 * get an ArrayList and starting the download and MD5sum check for the
	 * different files
	 * 
	 * @param args
	 * @throws Throwable 
	 */
	public boolean downloadAndMD5sumCheck(ArrayList<Package> args,DPKGPackageManager pkgmgr)
			throws PackageManagerException {
		LinkedList<File> files = new LinkedList<File>();
		LinkedList<Package> packs = new LinkedList<Package>();
		boolean ret=false;
		int len = args.size();
		Downloader d = new Downloader(args, files, packs,pkgmgr);
		checksum ch = new checksum(files, packs, len);
		d.start();
		ch.start();
		try {
			ch.join();
			UncaughtExceptionHandler UncaughtException = ch.getUncaughtExceptionHandler();
			UncaughtException =d.getUncaughtExceptionHandler();
			if(UncaughtException != null)
				throw new PackageManagerException(UncaughtException.toString());
			else{
				ret=true;
			}
		} catch (Throwable e) {
			throw new PackageManagerException (e);
		}

		return(ret);
	}
}

/**
 * downloads the different files from the server
 * 
 * @author tauschfn
 * 
 */
class Downloader extends Thread implements Thread.UncaughtExceptionHandler {

	private ArrayList<Package> packages;

	private LinkedList<File> files;

	private LinkedList<Package> packs;

	private String archivesDir;

	private String partialDir;
	
	private DPKGPackageManager pkgmgr;

	public Downloader(ArrayList<Package> pack, LinkedList<File> file,
			LinkedList<Package> packs,DPKGPackageManager pkgmgr) {
		this.pkgmgr=pkgmgr;
		this.packages = pack;
		this.files = file;
		this.packs = packs;
		this.archivesDir = PreferenceStoreHolder.getPreferenceStoreByName(
				"tempPackageManager").getPreferenceAsString("archivesDir", null);
		this.partialDir = PreferenceStoreHolder.getPreferenceStoreByName(
				"tempPackageManager").getPreferenceAsString("partialDir", null);
	}

	public void run() {
		for (int i = 0; i < packages.size(); i++) {
			synchronized (files) {
				String[] FileNamesForCheckIfAlreadyDownloaded = new FileName()
						.getLocationsForDownload(packages.get(i).getFilename(), packages
								.get(i).getServerPath(), archivesDir);
				String[] FileNames = new FileName().getLocationsForDownload(packages
						.get(i).getFilename(), packages.get(i).getServerPath(), partialDir);

				if (new File(FileNamesForCheckIfAlreadyDownloaded[1]).isFile()
						&& new File(FileNamesForCheckIfAlreadyDownloaded[1])
								.renameTo(new File(FileNames[1]))) {

					files.addLast(new File(FileNames[1]));
					packs.addLast(packages.get(i));
					files.notify();
				} else {

					try {
						URL url = new URL(FileNames[0]);
						InputStream in = url.openStream();
						FileOutputStream out = new FileOutputStream(FileNames[1]);
						int buflength = 4096;
						//max /4 weil 4094==4kb 
						double maxsize=pkgmgr.getMaxVolumeinByte();
//						durch 2(weil 50%des ganzen gleich download restliche 50% install!)
						int maxProgress=new Double(pkgmgr.getMaxProgress()*0.6).intValue();
						byte[] buf = new byte[buflength];
						int len;
						int anzahl=0;
						int beforeStarting=pkgmgr.getActprogress();
						double leneee=0;
						while ((len = in.read(buf)) > 0) {
							out.write(buf, 0, len);
							anzahl++;
							leneee+=len;
							if(anzahl%25==0){
								pkgmgr.setActprogressPlusX((beforeStarting+new Double((leneee/maxsize)*maxProgress).intValue()),new Double(leneee/1024).intValue(),new Double(packages.get(i).getSize()/1024).intValue(),packages.get(i).getName());								
							}
						}
						pkgmgr.setActprogressPlusX((beforeStarting+new Double((leneee/maxsize)*maxProgress).intValue()),new Double(leneee/1024).intValue(),new Double(packages.get(i).getSize()/1024).intValue(),packages.get(i).getName());
						in.close();
						out.close();
						files.addLast(new File(FileNames[1]));
						packs.addLast(packages.get(i));
						files.notify();
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}
	}

	public void uncaughtException(Thread t, Throwable e) {

	}

}

/**
 * 
 * compare the MD5sum which is in the package with one which is made of the .deb
 * file
 * 
 * @author tauschfn
 * 
 */
class checksum extends Thread implements Thread.UncaughtExceptionHandler {

	private LinkedList<File> files;

	private LinkedList<Package> packages;

	private int len;

	public checksum(LinkedList<File> file, LinkedList<Package> packages, int len) {
		this.files = file;
		this.packages = packages;
		this.len = len;
	}

	public void run() {
		for (int n = 0; n < len; n++) {
			synchronized (files) {
				if (files.size() < 1) {
					try {
						files.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
				MessageDigest md;
				try {
					md = MessageDigest.getInstance("MD5");
					FileInputStream in = new FileInputStream(files.get(0));
					int len;
					byte[] buf = new byte[4096];
					while ((len = in.read(buf)) > 0) {

						md.update(buf, 0, len);
					}

					byte[] result = md.digest();
					String md5sum = "";
					for (int i = 0; i < result.length; ++i) {

						md5sum = md5sum + toHexString(result[i]);
					}
					md.reset();
					in.close();

					if (packages.get(0).getMD5sum().equalsIgnoreCase(md5sum)) {
						File file = files.get(0);
						File parentFile = file.getParentFile();
						if (parentFile != null && parentFile.exists())
							parentFile = parentFile.getParentFile();
						String testFileName = parentFile.getPath() + File.separator
								+ file.getName();
						if (!(files.get(0).renameTo(new File(testFileName))))

							uncaughtException(this, new Throwable(PreferenceStoreHolder
									.getPreferenceStoreByName("Screen").getPreferenceAsString(
											"checksum.moveFile",
											"Entry not found for checksum.moveFile")
									+ files.get(0).getPath()));
					} else {
						uncaughtException(this, new Throwable(PreferenceStoreHolder
								.getPreferenceStoreByName("Screen").getPreferenceAsString(
										"checksum.md5different",
										"Entry not found for checksum.md5different")));
						files.get(0).delete();
					}
					files.remove(0);
					packages.remove(0);
				} catch (NoSuchAlgorithmException e1) {
					e1.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 
	 * @param b
	 * @return returns a "MD5SUM"
	 */
	public String toHexString(byte b) {
		int value = (b & 0x7F) + (b < 0 ? 128 : 0);
		String ret = (value < 16 ? "0" : "");
		ret += Integer.toHexString(value).toUpperCase();
		return ret;
	}

	public void uncaughtException(Thread t, Throwable e) {

	}

}

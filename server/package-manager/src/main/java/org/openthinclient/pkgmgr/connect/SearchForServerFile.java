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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.util.dpkg.UrlAndFile;

import com.levigo.util.preferences.PreferenceStoreHolder;

public class SearchForServerFile {

	public SearchForServerFile() {
	}

	private List<UrlAndFile> updateUrlAndFile;
	private static final String storeName = "tempPackageManager";
	private final String listsDir = PreferenceStoreHolder
			.getPreferenceStoreByName(storeName).getPreferenceAsString("listsDir",
					null);
	private final String sourcesList = PreferenceStoreHolder
			.getPreferenceStoreByName(storeName).getPreferenceAsString("sourcesList",
					null);
	private static final Logger logger = Logger
			.getLogger(SearchForServerFile.class);

	/**
	 * loads the Packages.gz files out of the sources.list, and download this
	 * files, and save it to the disk.
	 * 
	 * @return List<UrlAndFile>
	 * @throws InterruptedException
	 * @throws PackageManagerException
	 */
	public List<UrlAndFile> checkForNewUpdatedFiles(PackageManager pm)
			throws PackageManagerException {
		getLines(pm);
		if (updateUrlAndFile.size() == 0) {
			updateUrlAndFile = null;
			return updateUrlAndFile;
		} else
			return updateUrlAndFile;

	}

	/**
	 * 
	 * @throws InterruptedException
	 * @throws PackageManagerException
	 */
	private void getLines(PackageManager pm) throws PackageManagerException {

		final Pattern p = Pattern
				.compile("\\s*deb\\s+(ftp://|http://)(\\S+)\\s+((\\S+\\s*)*)(./){0,1}");
		// this means that the line which is significant for us should look like
		// this:
		// " deb hereStandsAnUrl hereStandsWhichFolder
		Matcher m;
		if (updateUrlAndFile == null)
			updateUrlAndFile = new ArrayList<UrlAndFile>();
		BufferedReader f;
		String protocol;
		String host;
		String shares;
		String adress;
		try {
			f = new BufferedReader(new FileReader(sourcesList));

			while ((protocol = f.readLine()) != null) {
				m = p.matcher(protocol);
				if (m.matches()) {
					protocol = m.group(1);
					host = m.group(2);
					if (m.group(3).trim().equalsIgnoreCase("./"))
						shares = "";
					else
						shares = m.group(3).trim();
					if (shares == null)
						adress = protocol + host;
					else {
						shares = shares.replace(" ", "/");
						if (!host.endsWith("/") && !shares.startsWith("/"))
							host = host + "/";
						adress = host + shares;
						while (adress.contains("//"))
							adress = adress.replace("//", "/");
						adress = protocol + adress;
					}
					if (!adress.endsWith("/"))
						adress = adress + "/";

					String changelogdir = adress;
					changelogdir = changelogdir.substring(changelogdir.indexOf("//") + 2);
					if (changelogdir.endsWith("/"))
						changelogdir = changelogdir.substring(0, changelogdir
								.lastIndexOf("/"));
					changelogdir = changelogdir.replace('/', '_');
					changelogdir = changelogdir.replaceAll("\\.", "_");
					changelogdir = changelogdir.replaceAll("-", "_");
					adress = adress + "Packages.gz";
					final NameFileLocation nfl = new NameFileLocation();
					try {
						final URL url = new URL(adress);
						final GZIPInputStream in = new GZIPInputStream(url.openStream());
						final String rename = new File(nfl.rename(adress, listsDir))
								.getCanonicalPath();
						final FileOutputStream out = new FileOutputStream(rename);
						final byte[] buf = new byte[4096];
						int len;
						while ((len = in.read(buf)) > 0)
							out.write(buf, 0, len);
						out.close();
						in.close();
						final File file = new File(rename);
						final UrlAndFile uaf = new UrlAndFile(protocol + host, file,
								changelogdir);
						updateUrlAndFile.add(uaf);
					} catch (final Exception e) {
						final String message = "URL: " + new URL(adress)
								+ " caused exception";
						if (null != pm) {
							logger.warn(message, e);
							pm.addWarning(message + "\n" + e.toString());
						} else
							logger.warn(message, e);
					}

					// } catch (final FileNotFoundException e) {
					// throw new PackageManagerException(e
					// + " "
					// + PreferenceStoreHolder.getPreferenceStoreByName("Screen")
					// .getPreferenceAsString("sourcesList.corrupt",
					// "Entry not found sourcesList.corrupt"));
					// } catch (final IOException e) {
					// throw new PackageManagerException(e);
					// }
				}
			}
			f.close();
		} catch (final FileNotFoundException e) {
			throw new PackageManagerException(e
					+ " "
					+ PreferenceStoreHolder.getPreferenceStoreByName("Screen")
							.getPreferenceAsString("sourcesList.corrupt",
									"Entry not found sourcesList.corrupt"));
		} catch (final IOException e) {
			throw new PackageManagerException(e);
		}
	}
}

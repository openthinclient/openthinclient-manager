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
package org.openthinclient.util.dpkg;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.openthinclient.pkgmgr.I18N;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.util.ar.AREntry;
import org.openthinclient.util.ar.ARInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class DPKGPackage extends Package {

	static final Logger logger = LoggerFactory.getLogger(Package.class);
   private List<File> files;
	private List<File> directories;
   private boolean packageManager;
   private String changelogDir;
   private String oldFolder;

   public DPKGPackage() {
		files = new ArrayList<>();
	}

	public void setPackageManager(boolean packageManager) {
		this.packageManager = packageManager;
	}

   private interface EntryCallback {
		void handleEntry(String s, InputStream inputstream) throws IOException, PackageManagerException;
	}

	private int findAREntry(String segmentName, EntryCallback callback,
			File archivePath) throws IOException, PackageManagerException {
		final ARInputStream ais = new ARInputStream(getPackageStream(archivePath));
		AREntry e;
		int callbackCount = 0;
		while ((e = ais.getNextEntry()) != null)
			if (e.getName().equals(segmentName)) {
				callback.handleEntry(e.getName(), ais);
				callbackCount++;
			}

		ais.close();

		return callbackCount;
	}

   private FileInputStream getPackageStream(File archivePath)
			throws IOException, PackageManagerException {
		final int lastSlashInName = getFilename().lastIndexOf("/");
		final String newFileName = getFilename().substring(lastSlashInName);
		File packageFile = new File(archivePath, newFileName);

		return new FileInputStream(packageFile);
	}

   public void install(final File rootPath,
			final List<InstallationLogEntry> log, File archivesPath,
			PackageManager pm) throws PackageManagerException {

		try {
			if (findAREntry("data.tar.gz", new EntryCallback() {
				public void handleEntry(String entry, InputStream ais)
						throws IOException, PackageManagerException {
					final TarInputStream tis = new TarInputStream(
							new GZIPInputStream(ais));
					TarEntry t;
					while ((t = tis.getNextEntry()) != null)
						installFile(tis, t, rootPath, log);
				}

			}, archivesPath) == 0) {
				final String errorMessage = I18N.getMessage("package.install.firstRuntimeException");
				if (pm != null) {
					pm.addWarning(errorMessage);
					logger.error(errorMessage);
				} else
					logger.error(errorMessage);
			}
			// throw new PackageManagerException(PreferenceStoreHolder
			// .getPreferenceStoreByName("Screen").getPreferenceAsString(
			// "DPKGPackage.unableToInstall",
			// "No entry found for package.getFiles.IOException"));
		} catch (final IOException e) {
			final String errorMessage = I18N.getMessage("package.install.IOException");
			if (pm != null) {
				pm.addWarning(errorMessage);
				logger.error(errorMessage, e);
			} else
				logger.error(errorMessage, e);
		}
	}

	@SuppressWarnings("unchecked")
	private void installFile(TarInputStream tis, TarEntry t, File rootPath,
			List<InstallationLogEntry> log) throws IOException,
			PackageManagerException {
		final String path = getRealPath((new File(rootPath, t.getFile().getPath()))
				.getAbsolutePath());
		final File absoluteFile = new File(path);
		if (null == files)
			files = new ArrayList<File>();
		if (System.getProperty("os.name").toUpperCase().contains("WINDOWS")
				&& t.getFile().getPath().contains("::"))
			throw new IOException();
		switch (t.getLinkFlag()){
			default :
				break;

			case 0 : // '\0'
			case 48 : // '0'
				final OutputStream os = new BufferedOutputStream(new FileOutputStream(
						absoluteFile));
				tis.copyEntryContents(os);
				os.close();
				log.add(new InstallationLogEntry(
						InstallationLogEntry.Type.FILE_INSTALLATION, absoluteFile));
				logger.info((new StringBuilder()).append("Installed ").append(
						absoluteFile).toString());
				files.add(absoluteFile);
				break;

			case 53 : // '5'
				if (!absoluteFile.exists()) {
					if (!absoluteFile.mkdir())
						throw new IOException((new StringBuilder()).append(
								"mkdir failed for ").append(absoluteFile).toString());
					log.add(new InstallationLogEntry(
							InstallationLogEntry.Type.DIRECTORY_CREATION, absoluteFile));
					logger.info((new StringBuilder()).append("Directory created: ")
							.append(absoluteFile).toString());
				}
				if (null == directories)
					directories = new ArrayList();
				directories.add(absoluteFile);
				break;

			case 49 : // '1'
			case 50 : // '2'
				// String SOFTLINK_TAG = ".#%softlink%#";
				logger.info((new StringBuilder()).append("Symlinking ").append(
						absoluteFile).append(" -> ").append(t.getLinkName()).toString());
				final String symlinkFile = (new StringBuilder()).append(absoluteFile)
						.append(".#%softlink%#").toString();
				final FileWriter w = new FileWriter(symlinkFile);
				w.write(t.getLinkName());
				w.close();
				log.add(new InstallationLogEntry(
						InstallationLogEntry.Type.SYMLINK_INSTALLATION, new File(
								symlinkFile)));
				files.add(new File(symlinkFile));
				break;
		}
	}

	public static String getRealPath(String path) throws PackageManagerException {
		final File file = new File(path);
		try {
			path = file.getCanonicalPath();
			path = path.replaceAll("\\./", "");
			return path;
		} catch (final IOException e) {
			e.printStackTrace();
			throw new PackageManagerException(e);
		}

	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("  Package: ").append(getName()).append("\n");
		sb.append("  Version: ").append(getVersion()).append("\n");
		sb.append("  Architecture: ").append(getArchitecture()).append("\n");
		sb.append("  Changed-By: ").append(getChangedBy()).append("\n");
		sb.append("  Date: ").append(getDate()).append("\n");
		sb.append("  Essential: ").append(isEssential()).append("\n");
		sb.append("  Is-Package-Manager: ").append(packageManager).append("\n");
		sb.append("  Distribution: ").append(getDistribution()).append("\n");
		sb.append("  Installed-Size: ").append(getInstalledSize()).append("\n");
		sb.append("  Maintainer: ").append(getMaintainer()).append("\n");
		sb.append("  Priority: ").append(getPriority()).append("\n");
		sb.append("  Section: ").append(getSection()).append("\n");
		sb.append("  MD5sum: ").append(getMd5sum()).append("\n");
		sb.append("  Description: \n").append(getDescription()).append("\n\n");
		sb.append("  Dependencies:\n");
		sb.append("    Depends: ").append(getDepends()).append("\n");
		sb.append("    Conflicts: ").append(getConflicts()).append("\n");
		sb.append("    Enhances: ").append(getEnhances()).append("\n");
		sb.append("    Pre-Depends: ").append(getPreDepends()).append("\n");
		sb.append("    Provides: ").append(getProvides()).append("\n");
		sb.append("    Recommends: ").append(getRecommends()).append("\n");
		sb.append("    Replaces: ").append(getReplaces()).append("\n");
		sb.append("    Suggests: ").append(getSuggests()).append("\n");
		return sb.toString();
	}

	public String forConflictsToString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("  Package: ").append(getName()).append("\n");
		sb.append("  Version: ").append(getVersion()).append("\n");
		sb.append("  Conflicts: ").append(getConflicts()).append("\n");
		sb.append("  Description: \n").append(getDescription()).append("\n\n");
		return sb.toString();
	}

   public List<File> getFileList() {
		return files;
	}

	public List<File> getDirectoryList() {
		return directories;
	}

   public void setFileList(List<File> fileList) {
		files = fileList;
	}

	public void setDirectoryList(List<File> directoryList) {
		directories = directoryList;
	}

   public boolean isPackageManager() {
		return packageManager;
	}

   public String getChangelogDir() {
		return changelogDir;
	}

	public void setChangelogDir(String changelogDir) {
		this.changelogDir = changelogDir;
	}

   public int compareTo(Package o) {
      final int c1 = getName().compareTo(o.getName());
      return c1 == 0 ? getVersion().compareTo(o.getVersion()) : c1;
   }

	public String getoldFolder() {
		return oldFolder;
	}

	public void setoldFolder(String rootDir) {
		this.oldFolder = rootDir;
	}

}

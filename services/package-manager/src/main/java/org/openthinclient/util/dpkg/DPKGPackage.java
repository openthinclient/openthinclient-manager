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

public class DPKGPackage implements Package {

	static final Logger logger = LoggerFactory.getLogger(Package.class);
	private static final long serialVersionUID = 0x2d33363938363032L;
	private String architecture;
	private String changedBy;
	private PackageReference conflicts;
	private String date;
	private PackageReference depends;
	private String description;
	private String distribution;
	private PackageReference enhances;
	private boolean essential;
	private List<File> files;
	private List<File> directories;
	private long installedSize;
	private String maintainer;
	private String name;
	private URL packageURL;
	private PackageReference preDepends;
	private String priority;
	private PackageReference provides;
	private PackageReference recommends;
	private PackageReference replaces;
	private String section;
	private PackageReference suggests;
	private Version version;
	private String filename;
	private String serverPath;
	private String md5sum;
	private boolean packageManager;
	private long size;
	private String changelogDir;
	private String shortDescription;
	private String oldFolder;
	private String license;

	public DPKGPackage() {
		files = new ArrayList<>();
	}

	public void setArchitecture(String architecture) {
		this.architecture = architecture;
	}

	public void setChangedBy(String changedBy) {
		this.changedBy = changedBy;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDistribution(String distribution) {
		this.distribution = distribution;
	}

	public void setEssential(boolean essential) {
		this.essential = essential;
	}

	public void setPackageManager(boolean packageManager) {
		this.packageManager = packageManager;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public void setInstalledSize(long installedSize) {
		this.installedSize = installedSize;
	}

	public void setMaintainer(String maintainer) {
		this.maintainer = maintainer;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public void setMd5sum(String md5sum) {
		this.md5sum = md5sum;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	public void setConflicts(PackageReference conflicts) {
		this.conflicts = conflicts;
	}

	public void setDepends(PackageReference depends) {
		this.depends = depends;
	}

	public void setEnhances(PackageReference enhances) {
		this.enhances = enhances;
	}

	public void setPreDepends(PackageReference preDepends) {
		this.preDepends = preDepends;
	}

	public void setProvides(PackageReference provides) {
		this.provides = provides;
	}

	public void setRecommends(PackageReference recommends) {
		this.recommends = recommends;
	}

	public void setReplaces(PackageReference replaces) {
		this.replaces = replaces;
	}

	public void setSuggests(PackageReference suggests) {
		this.suggests = suggests;
	}

	public void setVersion(Version version) {
		this.version = version;
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

	public PackageReference getConflicts() {
		return conflicts;
	}

	public PackageReference getDepends() {
		return depends;
	}

	public List<File> getFiles(File archivesPath, PackageManager pm)
			throws PackageManagerException {
		if (null == files) {
			files = new ArrayList<File>();
			try {
				if (findAREntry("data.tar.gz", new EntryCallback() {
					public void handleEntry(String entry, InputStream ais)
							throws IOException {
						final TarInputStream tis = new TarInputStream(new GZIPInputStream(
								ais));
						TarEntry t;
						while ((t = tis.getNextEntry()) != null)
							if (t.getLinkFlag() != TarEntry.LF_DIR)
								files.add(t.getFile());
					}
				}, archivesPath) == 0) {
					final String errorMessage = I18N.getMessage("package.getFiles.firstRuntimeException");
					if (pm != null) {
						pm.addWarning(errorMessage);
						logger.error(errorMessage);
					} else
						logger.error(errorMessage);

				}
			} catch (final IOException e) {
				final String errorMessage = I18N.getMessage("package.getFiles.IOException");
				if (pm != null) {
					pm.addWarning(errorMessage);
				}
				logger.error(errorMessage,e);
			}
		}
		return files;
	}

	public String getName() {
		return name;
	}

	private FileInputStream getPackageStream(File archivePath)
			throws IOException, PackageManagerException {
		final int lastSlashInName = filename.lastIndexOf("/");
		final String newFileName = filename.substring(lastSlashInName);
		File packageFile = new File(archivePath, newFileName);
//		if (null != packageFile)
			return new FileInputStream(packageFile);

		// the following code has actually never been reached, as the previous non-null check of the package file always evaluated to true.
//		if (null != packageURL) {
//			// final InputStream urlStream = packageURL.openStream();
//			final InputStream urlStream = DownloadManagerFactory.create(pm.getConfiguration().getProxyConfiguration())
//					.getInputStream(packageURL);
//			packageFile = new File((new StringBuilder()).append(getName()).append(
//					".deb").toString());
//			final OutputStream fileStream = new FileOutputStream(packageFile);
//			final byte buffer[] = new byte[10240];
//			for (int read = 0; (read = urlStream.read(buffer)) > 0;)
//				fileStream.write(buffer, 0, read);
//
//			urlStream.close();
//			fileStream.close();
//			return new FileInputStream(packageFile);
//		} else {
//			final String errorMessage = I18N.getMessage("package.getPackageStream.packageURLIsNull");
//			if (pm != null) {
//				pm.addWarning(errorMessage);
//				logger.error(errorMessage);
//			} else
//				logger.error(errorMessage);
//			throw new FileNotFoundException();
//		}

	}

	public PackageReference getPreDepends() {
		return preDepends;
	}

	public PackageReference getProvides() {
		return provides;
	}

	public Version getVersion() {
		return version;
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
		final StringBuffer sb = new StringBuffer();
		sb.append("  Package: ").append(name).append("\n");
		sb.append("  Version: ").append(version).append("\n");
		sb.append("  Architecture: ").append(architecture).append("\n");
		sb.append("  Changed-By: ").append(changedBy).append("\n");
		sb.append("  Date: ").append(date).append("\n");
		sb.append("  Essential: ").append(essential).append("\n");
		sb.append("  Is-Package-Manager: ").append(packageManager).append("\n");
		sb.append("  Distribution: ").append(distribution).append("\n");
		sb.append("  Installed-Size: ").append(installedSize).append("\n");
		sb.append("  Maintainer: ").append(maintainer).append("\n");
		sb.append("  Priority: ").append(priority).append("\n");
		sb.append("  Section: ").append(section).append("\n");
		sb.append("  MD5sum: ").append(md5sum).append("\n");
		sb.append("  Description: \n").append(description).append("\n\n");
		sb.append("  Dependencies:\n");
		sb.append("    Depends: ").append(depends).append("\n");
		sb.append("    Conflicts: ").append(conflicts).append("\n");
		sb.append("    Enhances: ").append(enhances).append("\n");
		sb.append("    Pre-Depends: ").append(preDepends).append("\n");
		sb.append("    Provides: ").append(provides).append("\n");
		sb.append("    Recommends: ").append(recommends).append("\n");
		sb.append("    Replaces: ").append(replaces).append("\n");
		sb.append("    Suggests: ").append(suggests).append("\n");
		return sb.toString();
	}

	public String forConflictsToString() {
		final StringBuffer sb = new StringBuffer();
		sb.append("  Package: ").append(name).append("\n");
		sb.append("  Version: ").append(version).append("\n");
		sb.append("  Conflicts: ").append(conflicts).append("\n");
		sb.append("  Description: \n").append(description).append("\n\n");
		return sb.toString();
	}

	public void setServerPath(String sePa) {
		serverPath = sePa;
	}

	public String getServerPath() {
		return serverPath;
	}

	public String getFilename() {
		return filename;
	}

	public String getMD5sum() {
		return md5sum;
	}

	public List<File> getFileList() {
		return files;
	}

	public List<File> getDirectoryList() {
		return directories;
	}

	public void setVersion(String s) {
		version = new Version(s);
	}

	public String getDescription() {
		return description;
	}

	public void setFileList(List<File> fileList) {
		files = fileList;
	}

	public void setDirectoryList(List<File> directoryList) {
		directories = directoryList;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isPackageManager() {
		return packageManager;
	}

	public String getLicense() {
		return license;
	}

	public long getSize() {
		return size;
	}

	public long getInstalledSize() {
		return installedSize;
	}

	public String getChangelogDir() {
		return changelogDir;
	}

	public void setChangelogDir(String changelogDir) {
		this.changelogDir = changelogDir;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public int compareTo(Package o) {
		final int c1 = getName().compareTo(o.getName());
		return c1 == 0 ? getVersion().compareTo(o.getVersion()) : c1;
	}

	public String getSection() {
		return section;
	}

	public String getPriority() {
		return priority;
	}

	public String getoldFolder() {
		return oldFolder;
	}

	public void setoldFolder(String rootDir) {
		this.oldFolder = rootDir;
	}

}

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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.connect.ConnectToServer;
import org.openthinclient.util.ar.AREntry;
import org.openthinclient.util.ar.ARInputStream;

import com.levigo.util.preferences.PreferenceStoreHolder;

public class DPKGPackage implements Package {

	static final Logger logger = Logger.getLogger(Package.class);
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
	private PackageManager pm;

	public DPKGPackage(List specLines, PackageManager pm) {
		files = new ArrayList<File>();
		String currentSection = null;
		final Map<String, String> controlTable = new HashMap<String, String>();
		for (final Iterator i = specLines.iterator(); i.hasNext();) {
			final String line = (String) i.next();
			currentSection = parseControlFileLine(controlTable, line, currentSection);
		}

		populateFromControlTable(controlTable);
		this.pm = pm;
	}

	public DPKGPackage(File packageFile, String archivesPath, PackageManager pm)
			throws IOException, PackageManagerException {
		files = new ArrayList<File>();
		verifyCompatibility(archivesPath);
		loadControlFile(archivesPath);
		this.pm = pm;
	}

	private static interface EntryCallback {

		public abstract void handleEntry(String s, InputStream inputstream)
				throws IOException, PackageManagerException;
	}

	private int findAREntry(String segmentName, EntryCallback callback,
			String archivePath) throws IOException, PackageManagerException {
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

	private boolean findControlFile(String fileName,
			final EntryCallback callback, String archivesPath) throws IOException,
			PackageManagerException {
		if (!fileName.startsWith("." + File.separator))
			fileName = "." + File.separator + fileName;

		final String matchName = fileName;
		return findAREntry("control.tar.gz", new EntryCallback() {
			public void handleEntry(String entry, InputStream ais)
					throws IOException, PackageManagerException {
				final TarInputStream tis = new TarInputStream(new GZIPInputStream(ais));
				TarEntry t;
				while ((t = tis.getNextEntry()) != null)
					if (t.getName().equals(matchName) && !t.isDirectory())
						callback.handleEntry(t.getName(), tis);
			}
		}, archivesPath) != 0;
	}

	public PackageReference getConflicts() {
		return conflicts;
	}

	public PackageReference getDepends() {
		return depends;
	}

	public List<File> getFiles(String archivesPath, PackageManager pm)
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
					String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName(
							"Screen").getPreferenceAsString(
							"package.getFiles.firstRuntimeException",
							"No entry found for package.getFiles.firstRuntimeException");
					if (pm != null) {
						pm.addWarning(errorMessage);
						logger.error(errorMessage);
					} else
						logger.error(errorMessage);

				}
				// pm.addWarning(PreferenceStoreHolder
				// .getPreferenceStoreByName("Screen").getPreferenceAsString(
				// "package.getFiles.firstRuntimeException",
				// "No entry found for package.getFiles.firstRuntimeException"));
				// throw new PackageManagerException(PreferenceStoreHolder
				// .getPreferenceStoreByName("Screen").getPreferenceAsString(
				// "package.getFiles.firstRuntimeException",
				// "No entry found for package.getFiles.firstRuntimeException"));
			} catch (final IOException e) {
				String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName(
						"Screen").getPreferenceAsString("package.getFiles.IOException",
						"No entry found for package.getFiles.IOException");
				if (pm != null) {
					pm.addWarning(errorMessage);
					logger.error(errorMessage);
				} else
					logger.error(errorMessage);
				e.printStackTrace();
				// throw new PackageManagerException(e);
			}
		}
		return files;
	}

	public String getName() {
		return name;
	}

	private FileInputStream getPackageStream(String archivePath)
			throws IOException, PackageManagerException {
		final int lastSlashInName = filename.lastIndexOf("/");
		final String newFileName = filename.substring(lastSlashInName);
		File packageFile = new File((new StringBuilder()).append(archivePath)
				.append(newFileName).toString());
		if (null != packageFile)
			return new FileInputStream(packageFile);
		if (null != packageURL) {
			// final InputStream urlStream = packageURL.openStream();
			final InputStream urlStream = new ConnectToServer(null)
					.getInputStream(packageURL);
			packageFile = new File((new StringBuilder()).append(getName()).append(
					".deb").toString());
			final OutputStream fileStream = new FileOutputStream(packageFile);
			final byte buffer[] = new byte[10240];
			for (int read = 0; (read = urlStream.read(buffer)) > 0;)
				fileStream.write(buffer, 0, read);

			urlStream.close();
			fileStream.close();
			return new FileInputStream(packageFile);
		} else {
			String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName(
					"Screen").getPreferenceAsString(
					"package.getPackageStream.packageURLIsNull",
					"No entry found for package.getPackageStream.packageURLIsNull");
			if (pm != null) {
				pm.addWarning(errorMessage);
				logger.error(errorMessage);
			} else
				logger.error(errorMessage);
			throw new FileNotFoundException();
		}

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
			final List<InstallationLogEntry> log, String archivesPath,
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
				String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName(
						"Screen").getPreferenceAsString(
						"package.install.firstRuntimeException",
						"No entry found for package.install.firstRuntimeException");
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
			String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName(
					"Screen").getPreferenceAsString("package.install.IOException",
					"No entry found for package.install.IOException");
			if (pm != null) {
				pm.addWarning(errorMessage);
				logger.error(errorMessage);
			} else
				logger.error(errorMessage);
			e.printStackTrace();
			// throw new PackageManagerException(e);
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

	private void loadControlFile(String archivesPath) throws IOException,
			PackageManagerException {
		final Map<String, String> controlTable = new HashMap<String, String>();
		if (!findControlFile("control", new EntryCallback() {
			/*
			 * @see org.openthinclient.util.dpkg.DEBPackage.EntryCallback#handleEntry(java.lang.String,
			 *      java.io.InputStream)
			 */
			public void handleEntry(String entry, InputStream ais) throws IOException {
				final BufferedReader br = new BufferedReader(new InputStreamReader(ais,
						"ISO8859-1"));
				String line;
				String currentSection = null;
				while ((line = br.readLine()) != null)
					currentSection = parseControlFileLine(controlTable, line,
							currentSection);
			}

		}, archivesPath)) {
			String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName(
					"Screen").getPreferenceAsString("package.invalidDebianpackage",
					"No entry found for package.invalidDebianpackage")
					+ " : "
					+ PreferenceStoreHolder
							.getPreferenceStoreByName("Screen")
							.getPreferenceAsString(
									"package.invalidDebianpackage.controlFile",
									"No entry found for package.invalidDebianpackage.controlFile");
			if (pm != null) {
				pm.addWarning(errorMessage);
				logger.error(errorMessage);
			} else
				logger.error(errorMessage);
		}
		// throw new IOException(PreferenceStoreHolder.getPreferenceStoreByName(
		// "Screen").getPreferenceAsString("package.invalidDebianpackage",
		// "No entry found for package.invalidDebianpackage")
		// + " : "
		// + PreferenceStoreHolder.getPreferenceStoreByName("Screen")
		// .getPreferenceAsString("package.invalidDebianpackage.controlFile",
		// "No entry found for package.invalidDebianpackage.controlFile"));

		populateFromControlTable(controlTable);
	}

	private String parseControlFileLine(final Map<String, String> controlTable,
			String line, String currentSection) {
		if (line.startsWith(" ")) {
			if (null == currentSection)
				logger.warn((new StringBuilder()).append(
						"Ignoring line starting with blank: no preceding section: \"")
						.append(line).append("\"").toString());
			else {
				if (line.equals(" ."))
					line = "\n";
				final String existing = controlTable.get(currentSection);
				if (existing != null)
					controlTable.put(currentSection, (new StringBuilder()).append(
							existing).append(line).toString());
				else
					controlTable.put(currentSection, line);
			}
		} else if (line.indexOf(": ") > 0) {
			final int index = line.indexOf(": ");
			final String section = line.substring(0, index);
			final String value = line.substring(index + 2);
			currentSection = section;
			if (section.equalsIgnoreCase("Description"))
				controlTable.put("Short-Description", value);
			else
				controlTable.put(section, value);
		} else
			logger.warn((new StringBuilder()).append("Ignoring unparseable line: \"")
					.append(line).append("\"").toString());
		return currentSection;
	}

	private PackageReference parsePackageReference(Map controlTable,
			String fieldName) {
		return new ANDReference(parseStringField(controlTable, fieldName, ""));
	}

	private String parseStringField(Map controlTable, String fieldName) {
		return parseStringField(controlTable, fieldName, null);
	}

	private String parseStringField(Map controlTable, String fieldName,
			String defaultValue) {
		if (controlTable.containsKey(fieldName))
			return (String) controlTable.get(fieldName);
		else
			return defaultValue;
	}

	private void populateFromControlTable(final Map<String, String> controlTable) {
		architecture = parseStringField(controlTable, "Architecture");
		changedBy = parseStringField(controlTable, "Changed-By");
		date = parseStringField(controlTable, "Date");
		description = parseStringField(controlTable, "Description");
		distribution = parseStringField(controlTable, "Distribution");
		essential = parseStringField(controlTable, "Essential", "no")
				.equalsIgnoreCase("yes");
		packageManager = parseStringField(controlTable, "PackageManagerFlag", "no")
				.equalsIgnoreCase("yes");
		size = Long.parseLong(parseStringField(controlTable, "Size", "-1"));
		installedSize = Long.parseLong(parseStringField(controlTable,
				"Installed-Size", "-1"));
		maintainer = parseStringField(controlTable, "Maintainer");
		name = parseStringField(controlTable, "Package");
		priority = parseStringField(controlTable, "Priority");
		section = parseStringField(controlTable, "Section");
		version = new Version(controlTable.get("Version"));
		md5sum = parseStringField(controlTable, "MD5sum");
		filename = parseStringField(controlTable, "Filename");
		shortDescription = parseStringField(controlTable, "Short-Description");
		conflicts = parsePackageReference(controlTable, "Conflicts");
		depends = parsePackageReference(controlTable, "Depends");
		enhances = parsePackageReference(controlTable, "Enhances");
		preDepends = parsePackageReference(controlTable, "Pre-Depends");
		provides = parsePackageReference(controlTable, "Provides");
		recommends = parsePackageReference(controlTable, "Recommends");
		replaces = parsePackageReference(controlTable, "Replaces");
		suggests = parsePackageReference(controlTable, "Suggests");
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

	private void verifyCompatibility(String archivesPath) throws IOException,
			PackageManagerException {
		if (findAREntry("debian-binary", new EntryCallback() {
			public void handleEntry(String entry, InputStream ais) throws IOException {
				final BufferedReader br = new BufferedReader(new InputStreamReader(ais,
						"ISO8859-1"));
				final String signature = br.readLine().trim();
				final String versionComponents[] = signature.split("\\.");
				if (versionComponents.length < 2) {
					String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName(
							"Screen").getPreferenceAsString("package.invalidDebianpackage",
							"No entry found for package.invalidDebianpackage")
							+ " : "
							+ PreferenceStoreHolder
									.getPreferenceStoreByName("Screen")
									.getPreferenceAsString(
											"package.invalidDebianpackage.cantParseVersion",
											"No entry found for package.invalidDebianpackage.cantParseVersion")
							+ PreferenceStoreHolder
									.getPreferenceStoreByName("Screen")
									.getPreferenceAsString(
											"package.invalidDebianpackage.versionNotSupported1",
											"No entry found for package.invalidDebianpackage.versionNotSupported1");
					if (pm != null) {
						pm.addWarning(errorMessage);
						logger.error(errorMessage);
					} else
						logger.error(errorMessage);
				}
				// throw new IOException(
				// PreferenceStoreHolder.getPreferenceStoreByName("Screen")
				// .getPreferenceAsString("package.invalidDebianpackage",
				// "No entry found for package.invalidDebianpackage")
				// + " : "
				// + PreferenceStoreHolder
				// .getPreferenceStoreByName("Screen")
				// .getPreferenceAsString(
				// "package.invalidDebianpackage.cantParseVersion",
				// "No entry found for package.invalidDebianpackage.cantParseVersion")
				// + PreferenceStoreHolder
				// .getPreferenceStoreByName("Screen")
				// .getPreferenceAsString(
				// "package.invalidDebianpackage.versionNotSupported1",
				// "No entry found for
				// package.invalidDebianpackage.versionNotSupported1"));
				if (Integer.parseInt(versionComponents[0]) > 2
						|| Integer.parseInt(versionComponents[0]) == 2
						&& Integer.parseInt(versionComponents[1]) > 0) {
					String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName(
							"Screen").getPreferenceAsString("package.invalidDebianpackage",
							"No entry found for package.invalidDebianpackage")
							+ PreferenceStoreHolder
									.getPreferenceStoreByName("Screen")
									.getPreferenceAsString(
											"package.invalidDebianpackage.versionNotSupported1",
											"No entry found for package.invalidDebianpackage.versionNotSupported1")
							+ " : "
							+ signature
							+ " "
							+ PreferenceStoreHolder
									.getPreferenceStoreByName("Screen")
									.getPreferenceAsString(
											"package.invalidDebianpackage.versionNotSupported2",
											"No entry found for package.invalidDebianpackage.versionNotSupported2");
					if (pm != null) {
						pm.addWarning(errorMessage);
						logger.error(errorMessage);
					} else
						logger.error(errorMessage);
				}
				//
				// throw new IOException(
				// PreferenceStoreHolder.getPreferenceStoreByName("Screen")
				// .getPreferenceAsString("package.invalidDebianpackage",
				// "No entry found for package.invalidDebianpackage")
				// + PreferenceStoreHolder
				// .getPreferenceStoreByName("Screen")
				// .getPreferenceAsString(
				// "package.invalidDebianpackage.versionNotSupported1",
				// "No entry found for
				// package.invalidDebianpackage.versionNotSupported1")
				// + " : "
				// + signature
				// + " "
				// + PreferenceStoreHolder
				// .getPreferenceStoreByName("Screen")
				// .getPreferenceAsString(
				// "package.invalidDebianpackage.versionNotSupported2",
				// "No entry found for
				// package.invalidDebianpackage.versionNotSupported2"));
			}
		}, archivesPath) == 0) {
			String errorMessage = PreferenceStoreHolder.getPreferenceStoreByName(
					"Screen").getPreferenceAsString("package.invalidDebianpackage",
					"No entry found for package.invalidDebianpackage")
					+ " : "
					+ PreferenceStoreHolder
							.getPreferenceStoreByName("Screen")
							.getPreferenceAsString(
									"package.invalidDebianpackage.controlFile",
									"No entry found for package.invalidDebianpackage.controlFile");
			if (pm != null) {
				pm.addWarning(errorMessage);
				logger.error(errorMessage);
			} else
				logger.error(errorMessage);
		}
		// throw new IOException(
		// PreferenceStoreHolder.getPreferenceStoreByName("Screen")
		// .getPreferenceAsString("package.invalidDebianpackage",
		// "No entry found for package.invalidDebianpackage")
		// + " : "
		// + PreferenceStoreHolder
		// .getPreferenceStoreByName("Screen")
		// .getPreferenceAsString(
		// "package.invalidDebianpackage.controlFile",
		// "No entry found for package.invalidDebianpackage.controlFile"));
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

	public Package getThis() {
		return this;
	}

	public String getoldFolder() {
		return oldFolder;
	}

	public void setoldFolder(String rootDir) {
		this.oldFolder = rootDir;
	}

}

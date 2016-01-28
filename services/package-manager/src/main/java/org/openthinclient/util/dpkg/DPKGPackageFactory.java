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
package org.openthinclient.util.dpkg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openthinclient.util.dpkg.DPKGPackage;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * creates a new DPKGPackage out of a inputstream or an textfile
 * @author tauschfn
 *
 */
public class DPKGPackageFactory {

	private static final Logger LOG = LoggerFactory.getLogger(DPKGPackageFactory.class);

	public List<Package> getPackage(File file) throws IOException {
		PackageSource pkg = new PackageSource(file);
		return (pkg.getPackageIndex());
	}

	public List<Package> getPackage(InputStream stream) throws IOException {
		PackageSource pkg = new PackageSource(stream);
		return (pkg.getPackageIndex());
	}
/**
 * creats a {@link DPKGPackage} out of a given inputstream or textfile
 * @author tauschfn
 *
 */
	private class PackageSource {

		private List<Package> packageIndex;

		public PackageSource(File cacheFile) throws IOException {
			FileInputStream fis = new FileInputStream(cacheFile);
			update(fis);
			fis.close();
		}

		public PackageSource(InputStream stream) throws IOException {
			update(stream);
		}

		/**
		 * Gets an inputstream from a file and made out of these a List of DPKG Packages which
		 * is available by the method getPackageIndex()
		 * 
		 * @param stream
		 * @throws IOException
		 */
		private void update(InputStream stream) throws IOException {
			List<String> lines = new ArrayList<String>();

			BufferedReader br = new BufferedReader(new InputStreamReader(stream));
			try {
				packageIndex = new ArrayList<Package>();

				String line;
				while ((line = br.readLine()) != null) {
					if (line.length() == 0) {
						packageIndex.add(createPackage(lines));
						lines.clear();
					} else
						lines.add(line);
				}
			} finally {
				br.close();
				stream.close();
			}
		}

	private Package createPackage(List<String> specLines) {

		String currentSection = null;
		final Map<String, String> controlTable = new HashMap<String, String>();
		for (final String line : specLines) {
			currentSection = parseControlFileLine(controlTable, line, currentSection);
		}

		final DPKGPackage dpkgPackage = new DPKGPackage();
		populateFromControlTable(dpkgPackage, controlTable);

		return dpkgPackage;
	}

	private String parseControlFileLine(final Map<String, String> controlTable,
																			String line, String currentSection) {
		if (line.startsWith(" ")) {
			if (null == currentSection)
				LOG.warn((new StringBuilder()).append(
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
			LOG.warn((new StringBuilder()).append("Ignoring unparseable line: \"")
							.append(line).append("\"").toString());
		return currentSection;
	}

	private void populateFromControlTable(DPKGPackage pkg, final Map<String, String> controlTable) {
		pkg.setArchitecture(parseStringField(controlTable, "Architecture"));

		pkg.setChangedBy(parseStringField(controlTable, "Changed-By"));
		pkg.setDate(parseStringField(controlTable, "Date"));
		pkg.setDescription(parseStringField(controlTable, "Description"));
		pkg.setDistribution(parseStringField(controlTable, "Distribution"));
		pkg.setEssential(parseStringField(controlTable, "Essential", "no")
						.equalsIgnoreCase("yes"));
		pkg.setLicense(parseStringField(controlTable, "License"));
		pkg.setSize(Long.parseLong(parseStringField(controlTable, "Size", "-1")));
		pkg.setInstalledSize(Long.parseLong(parseStringField(controlTable,
						"Installed-Size", "-1")));
		pkg.setMaintainer(parseStringField(controlTable, "Maintainer"));
		pkg.setName(parseStringField(controlTable, "Package"));
		pkg.setPriority(parseStringField(controlTable, "Priority"));
		pkg.setSection(parseStringField(controlTable, "Section"));
		pkg.setVersion(new Version(controlTable.get("Version")));
		pkg.setMd5sum(parseStringField(controlTable, "MD5sum"));
		pkg.setFilename(parseStringField(controlTable, "Filename"));
		pkg.setShortDescription(parseStringField(controlTable, "Short-Description"));
		pkg.setConflicts(parsePackageReference(controlTable, "Conflicts"));
		pkg.setDepends(parsePackageReference(controlTable, "Depends"));
		pkg.setEnhances(parsePackageReference(controlTable, "Enhances"));
		pkg.setPreDepends(parsePackageReference(controlTable, "Pre-Depends"));
		pkg.setProvides(parsePackageReference(controlTable, "Provides"));
		pkg.setRecommends(parsePackageReference(controlTable, "Recommends"));
		pkg.setReplaces(parsePackageReference(controlTable, "Replaces"));
		pkg.setSuggests(parsePackageReference(controlTable, "Suggests"));
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

	/**
		 * 
		 * @return the List packageIndex which is created out of the given arguments
		 */
		public List<Package> getPackageIndex() {
			return packageIndex;
		}

	}
}

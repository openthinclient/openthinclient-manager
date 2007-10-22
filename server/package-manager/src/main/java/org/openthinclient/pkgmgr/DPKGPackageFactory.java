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
package org.openthinclient.pkgmgr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.openthinclient.util.dpkg.DPKGPackage;
import org.openthinclient.util.dpkg.Package;

/**
 * creates a new DPKGPackage out of a inputstream or an textfile
 * @author tauschfn
 *
 */
public class DPKGPackageFactory {

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
						Package pkg = new DPKGPackage(lines).getThis();
						packageIndex.add(pkg);
						lines.clear();
					} else
						lines.add(line);
				}
			} finally {
				br.close();
				stream.close();
			}
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

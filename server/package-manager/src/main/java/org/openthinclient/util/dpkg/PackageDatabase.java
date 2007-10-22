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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openthinclient.pkgmgr.PackageManagerException;

import com.levigo.util.preferences.PreferenceStoreHolder;

/**
 * The PackageDatabase represents a snapshot of the current installation state
 * of a system.
 * 
 * @author levigo
 */
public class PackageDatabase implements Serializable {
	private static final long serialVersionUID = 3761126046166234677L;

	private static final Logger logger = Logger.getLogger(PackageDatabase.class);

	/**
	 * The lock server just holds open a socket which allows peers to verify the
	 * validity of a lock file.
	 * 
	 * FIXME: NIO Locking ist echt ehrlich eleganter
	 */
	private static class LockFile {
		private ServerSocket s;

		private Thread serverThread;

		private boolean goAway = false;

		@SuppressWarnings("unused")
		private final File lockFile;

		private LockFile(File lockTarget, File lockFile) throws IOException {
			this.lockFile = lockFile;

			if (lockFile.exists()) {
				// uh-oh, the lock file exists. lets see if the locker is still
				// active
				BufferedReader br = new BufferedReader(new InputStreamReader(
						new FileInputStream(lockFile)));
				String line = br.readLine();
				if (null == line)
					throw new IOException("The lock file at " + lockFile
							+ " is broken. Please remove it manually.");
				InetAddress host = InetAddress.getByName(line.trim());
				line = br.readLine();
				if (null == line)
					throw new IOException("The lock file at " + lockFile
							+ " is broken. Please remove it manually.");
				int port = Integer.parseInt(line.trim());
				br.close();

				// try to connect to blocker
				try {
					new Socket(host, port).close();
					throw new IOException("The file at " + lockTarget + " is locked by "
							+ lockFile + ".");
				} catch (IOException e) {
					// connect failed. is the server down?
					logger.warn("The lock on " + lockTarget + " held by " + host
							+ " seems to be stale. Removing it.");
				}
			}

			startLockServer();
			writeLockFile(lockTarget, lockFile);

			startThread();
		}

		/**
		 * @param lockTarget
		 * @param lockFile
		 * @throws FileNotFoundException
		 * @throws IOException
		 * @throws UnknownHostException
		 */
		private void writeLockFile(File lockTarget, File lockFile)
				throws FileNotFoundException, IOException, UnknownHostException {
			OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(
					lockFile));
			String toString = InetAddress.getLocalHost().getCanonicalHostName();
			w.write(toString);
			w.write("\n");
			w.write(Integer.toString(s.getLocalPort()));
			w.write("\n");
			w.close();
			// verify it's me
			// uh-oh, the lock file exists. lets see if the locker is still
			// active

			if (!lockFile.isFile())
				writeLockFile(lockTarget, lockFile);

			logger.info("lockfile: " + lockFile + " locktarget: " + lockTarget);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(lockFile)));
			InetAddress host = InetAddress.getByName(br.readLine().trim());
			int port = Integer.parseInt(br.readLine().trim());
			br.close();
			if (!host.equals(InetAddress.getLocalHost()) || port != s.getLocalPort()) {

				try {
					s.close();
				} catch (IOException e) {
					// ignore
				}
				throw new IOException("Locking of " + lockTarget
						+ " failed: wasn't locked when I checked, but locker isn't me?!");
			}
		}

		/**
		 * 
		 */
		private void startThread() {
			serverThread = new Thread("Lock file server at " + s.getLocalPort()) {
				public void run() {
					while (!goAway) {
						try {
							// we don't actually talk to the peer
							s.accept().close();
						} catch (IOException e) {
							// ignore
						}
					}
				}
			};
			serverThread.start();
		}

		/**
		 * @throws IOException
		 */
		private void startLockServer() throws IOException {
			int startPort = 34567;
			int tries = 10;
			while (tries > 0) {
				try {
					s = new ServerSocket(startPort);
					s.setReuseAddress(true);
				} catch (IOException e) {
					tries--;
					startPort += Math.random() * 100;
				}
			}
			if (null == s)
				throw new IOException("Can't create lock server");
		}

		public void unlock() {
			if (null != s) {
				goAway = true;
				try {
					s.close();
				} catch (IOException e) {
					// ignore
				}

			}
		}

		public boolean isLocked() {
			return s != null && s.isBound() && !s.isClosed();
		}
	}

	private ArrayList<Package> packages;

	/**
	 * A map of virtual packages. It contains keys for all real as well as virtual
	 * package names.
	 */
	private transient Map<String, Package> providedPackages;

	private transient Map<File, Package> installedFiles;

	private transient LockFile lock;

	private transient File location;

	/**
	 * Keep your grubby fingers off of my constructor!
	 */
	private PackageDatabase() {
	}

	/**
	 * 
	 * @param databaseLocation
	 * @return a PackageDatabase with which the diferent methods could be made
	 * @throws IOException
	 */
	public static PackageDatabase open(File databaseLocation) throws IOException {
		File databaseDirectory = databaseLocation.getParentFile();

		if (!databaseDirectory.canWrite())
			throw new IOException("Can't write to " + databaseDirectory);

		// this will throw an IOException if the lock fails.
		LockFile lockFile = new LockFile(databaseLocation, new File(
				databaseLocation.getCanonicalPath() + ".lock"));

		if (databaseLocation.exists()) {
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
						databaseLocation));
				PackageDatabase db = (PackageDatabase) ois.readObject();
				ois.close();
				db.setLock(lockFile);
				db.setLocation(databaseLocation);
				logger.info("PackageDatabase at " + databaseLocation + " opened");
				return db;
			} catch (Throwable e) {
				lockFile.unlock();
				throw new IOException("Package database seems to be corrupt: " + e);
			}
		} else {
			PackageDatabase db = new PackageDatabase();
			db.setLock(lockFile);
			db.setLocation(databaseLocation);
			logger.info("New PackageDatabase at " + databaseLocation + " created");
			// packages=null;
			db.setPackages();
			return db;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if (lock.isLocked()) {
			logger
					.warn("Please close the PackageDatabase before dropping it on the floor");
			close();
		}
	}

	/**
	 * Save the database to the given loaction
	 * 
	 * @throws IOException
	 */
	public void save() throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				location));
		oos.writeObject(this);
		oos.close();
	}

	/**
	 * Close the package database (without saving it!)
	 */
	public void close() {

		logger.info("PackageDatabase at " + location + " closed");

		lock.unlock();

	}

	private void setLock(LockFile lock) {
		this.lock = lock;
	}

	private void setLocation(File location) {
		this.location = location;
	}
	/**
	 * 
	 * @param name
	 * @return TRUE ONLY if the package is saved in the database otherwise FALSE
	 */
	public boolean isPackageInstalled(String name) {
		return getProvidedPackages().containsKey(name);
	}
/**
 * 
 * @param name
 * @return TRUE ONLY if the package is saved in the database otherwise FALSE
 */
	public boolean isPackageInstalledDontVerifyVersion(String name) {
		if (null == getPackage(name))
			return (false);
		else
			return (true);
	}

	/**
	 * Get a map of all package names (virtual and non-virtual) to their providing
	 * package.
	 * 
	 * @return
	 */
	public Map<String, Package> getProvidedPackages() {
		// lazy initialization of virtual package map
		if (null == providedPackages) {
			// build map of installed features and files
			providedPackages = new HashMap<String, Package>();
			for (Package pkg : getPackages()) {
				providedPackages.put(pkg.getName(), pkg);
				if (pkg.getProvides() instanceof ANDReference)
					for (PackageReference r : ((ANDReference) pkg.getProvides())
							.getRefs())
						providedPackages.put(r.getName(), pkg);
				else
					providedPackages.put(pkg.getProvides().getName(), pkg);
			}
		}
		return providedPackages;
	}
/**
 * 
 * @return a collection of all packagtes which are isaved in the database
 */
	@SuppressWarnings("unchecked")
	public Collection<Package> getPackages() {
		if (null == packages)
			return Collections.EMPTY_LIST;
		return packages;
	}
/**
 * 
 * @return a map of the sinstalled Files and Packages
 * @throws PackageManagerException
 */
	public Map<File, Package> getInstalledFileMap()
			throws PackageManagerException {
		// lazy initialization of file package map
		if (null == installedFiles) {
			// build map of installed features and files
			installedFiles = new HashMap<File, Package>();
			for (Package pkg : getPackages())
				for (File f : pkg.getFiles(PreferenceStoreHolder
						.getPreferenceStoreByName("tempPackageManager")
						.getPreferenceAsString("installDir", null)))
					installedFiles.put(f, pkg);
		}

		return installedFiles;
	}

	public Package getPackageOwningFile(File f) throws PackageManagerException {
		return getInstalledFileMap().get(f);
	}

	/**
	 * adds a given package pkg to the database
	 * 
	 * @param pkg
	 */
	public void addPackage(Package pkg) {
		if (null == packages)
			packages = new ArrayList<Package>();
		if (isPackageInstalled(pkg.getName())) {
			if (-1 == getPackage(pkg.getName()).getVersion().compareTo(
					pkg.getVersion()))
				packages.remove(getPackage(pkg.getName()));
			packages.add(pkg);
		} else
			packages.add(pkg);
		// kill cache
		installedFiles = null;
		providedPackages = null;
	}
/**
 * SHOULD ONLY USED FOR THE DEBIAN DATABASE, NO VERIFICATION OF THE VERSION
 * @param pkg
 */
	public void addPackageDontVerifyVersion(Package pkg) {
		if (null == packages)
			packages = new ArrayList<Package>();
		Package temp = null;
		for (Package pkg2 : packages)
			if (pkg2.getName().equalsIgnoreCase(pkg.getName()))
				temp = pkg2;
		if (temp == null || -1 == temp.getVersion().compareTo(pkg.getVersion()))
			packages.add(pkg);

		// kill cache
		installedFiles = null;
		providedPackages = null;
	}

	/**
	 * 
	 * @param name
	 * @return the Package which has the given name
	 */
	public Package getPackage(String name) {
		if (packages == null)
			return (null);
		for (int i = 0; i < packages.size(); i++)
			if (packages.get(i).getName().trim().equalsIgnoreCase(name.trim()))
				return (packages.get(i));
		return (null);
	}

	/**
	 * is used for Virtual Packages the parameter is an String because virtual
	 * Packages couldn't be Packages
	 * 
	 * @param provided
	 * @return a list of Packages which are Providing these package given as
	 *         String
	 */
	public List<Package> getProvidesPackages(String provided) {
		List<Package> providePackages = new LinkedList<Package>();
		for (int i = 0; i < packages.size(); i++)
			if (packages.get(i).getProvides().toString().trim().equalsIgnoreCase(
					provided.trim()))
				providePackages.add(packages.get(i));

		return (providePackages);

	}

	/**
	 * 
	 * @param pack
	 * @return a list of Packages which are dependency of the given Package pack
	 */
	public List<Package> getDependency(Package pack) {
		ArrayList<Package> remove = new ArrayList<Package>();
		for (Package pkg : packages) {
			if (pkg.getDepends().matches(pack)) {
				remove.add(pkg);
			}
			if (pkg.getPreDepends().matches(pack)) {
				remove.add(pkg);
			}
		}
		return (remove);
	}

	/**
	 * 
	 * @param pkg
	 * @return TRUE only if the remove was accomplished otherwise FALSE
	 */
	public boolean removePackage(Package pkg) {
		Package pack = getPackage(pkg.getName());
		boolean b = false;

		if (providedPackages != null) {
			if (pack == providedPackages.remove(pkg.getName())
					&& packages.remove(pack))
				b = true;
		} else if (packages.remove(pack))
			b = true;
		try {
			save();
		} catch (IOException e) {
			System.err.println(PreferenceStoreHolder.getPreferenceStoreByName(
					"Screen").getPreferenceAsString("packageDatabase.errorOnSavingDB",
					"No entry found for packageDatabase.errorOnSavingDB"));
			b = false;
			e.printStackTrace();
		}
		return (b);
	}

	@SuppressWarnings("unchecked")
	/**
	 * only used when no packages available in the database so the packages
	 * variable is set to an empty list so that no Nullpointer exception could be
	 * thrown
	 */
	private void setPackages() {
		this.packages = new ArrayList<Package>(Collections.EMPTY_LIST);
	}
}

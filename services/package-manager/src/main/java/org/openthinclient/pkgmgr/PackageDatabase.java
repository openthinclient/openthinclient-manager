package org.openthinclient.pkgmgr;

import org.openthinclient.util.dpkg.*;
import org.openthinclient.util.dpkg.Package;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PackageDatabase {
	void save() throws IOException;

	void close();

	boolean isPackageInstalled(String name);

	boolean isPackageInstalledDontVerifyVersion(String name);

	Map<String, Package> getProvidedPackages();

	@SuppressWarnings("unchecked")
	Collection<Package> getPackages();

	void addPackage(Package pkg);

	void addPackageDontVerifyVersion(Package pkg);

	Package getPackage(String name);

	List<Package> getProvidesPackages(String provided);

	List<Package> getDependency(Package pack);

	boolean removePackage(Package pkg);
}

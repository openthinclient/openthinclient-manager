package org.openthinclient.pkgmgr.impl;

import org.junit.Ignore;
import org.junit.Test;
import org.openthinclient.pkgmgr.PackageDatabase;
import org.openthinclient.pkgmgr.PackageDatabaseFactory;
import org.openthinclient.util.dpkg.*;
import org.openthinclient.util.dpkg.Package;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractPackageDatabaseTestBase {

  private final PackageDatabaseFactory packageDatabaseFactory;

  public AbstractPackageDatabaseTestBase(PackageDatabaseFactory packageDatabaseFactory) {
    this.packageDatabaseFactory = packageDatabaseFactory;
  }

  @Test
  public void testCreateNewDatabaseAndSingleEntry() throws Exception {

    final Path tempFile = createTemporaryFile();

    PackageDatabase database = packageDatabaseFactory.create(tempFile);

    assertEquals(0, database.getPackages().size());

    database.addPackage(createSamplePackage("foo", "2.0-1"));

    assertEquals(1, database.getPackages().size());
    database.save();
    database.close();

    // reopen the package database
    database = packageDatabaseFactory.create(tempFile);

    assertEquals(1, database.getPackages().size());
  }

  @Test
    public void testAddPackageInDifferentVersions() throws Exception {

    final Path tempFile = createTemporaryFile();

    PackageDatabase database = packageDatabaseFactory.create(tempFile);

    database.addPackage(createSamplePackage("foo", "1.0-1"));

    assertEquals(1, database.getPackages().size());

    database.addPackage(createSamplePackage("foo", "1.0-2"));

    assertEquals(1, database.getPackages().size());

    assertEquals(new Version("1.0-2"), database.getPackages().iterator().next().getVersion());
  }


  @Test
  public void testAddAndRemovePackages() throws Exception {

    final Path tempFile = createTemporaryFile();

    PackageDatabase database = packageDatabaseFactory.create(tempFile);

    database.addPackageDontVerifyVersion(createSamplePackage("foo", "1.0-1"));

    assertEquals(1, database.getPackages().size());

    // FIXME the existing serialization DB behaves this way, BUT THAT FEELS JUST WRONG!
    // when providing a package object, only the appropriate version should be deleted!
    database.removePackage(createSamplePackage("foo", "1.0-2"));

    assertEquals(0, database.getPackages().size());
  }


  @Test
  public void testAddPackageDontVerifyVersionInDifferentVersions() throws Exception {

    final Path tempFile = createTemporaryFile();

    PackageDatabase database = packageDatabaseFactory.create(tempFile);

    database.addPackageDontVerifyVersion(createSamplePackage("foo", "1.0-1"));

    assertEquals(1, database.getPackages().size());

    database.addPackageDontVerifyVersion(createSamplePackage("foo", "1.0-2"));

    assertEquals(2, database.getPackages().size());
  }

  // FIXME this is too stupid! The serialization db DOES check the version,
  // but only adds new packages if the given package is newer than the existing.
  @Ignore
  @Test
  public void testAddPackageDontVerifyVersionInDifferentVersionsNewerVersionFirst() throws Exception {

    final Path tempFile = createTemporaryFile();

    PackageDatabase database = packageDatabaseFactory.create(tempFile);

    database.addPackageDontVerifyVersion(createSamplePackage("foo", "1.0-2"));

    assertEquals(1, database.getPackages().size());

    database.addPackageDontVerifyVersion(createSamplePackage("foo", "1.0-1"));

    assertEquals(2, database.getPackages().size());
  }

  @Test
  public void testCreateDatabaseWithManyEntries() throws Exception {

    final PackageDatabase database = createDatabaseWithSampleData();

    database.getPackages().stream().map(e->e.getName() + " " + e.getVersion()).forEach(System.err::println);

    // the all packages lists contains packages with the same name but different versions
    assertEquals(23, database.getPackages().size());

    // as the provided packages are represented within a map, only one single version is made available.
    assertEquals(21, database.getProvidedPackages().size());
  }

  @Test
  public void testDatabaseGetPackage() throws Exception {

    final PackageDatabase database = createDatabaseWithSampleData();

    final org.openthinclient.util.dpkg.Package tcosPackage = database.getPackage("tcos-libs");

    assertNotNull(tcosPackage);
    assertEquals(new Version("2.0-14"), tcosPackage.getVersion());

  }

  @Test
  public void testDatabaseGetProvidesPackage() throws Exception {

    final PackageDatabase database = createDatabaseWithSampleData();

    final List<Package> packages = database.getProvidesPackages("ica-client");

    assertNotNull(packages);
    assertEquals(1, packages.size());

    assertEquals("ica-client-12", packages.get(0).getName());


    assertEquals(0, database.getProvidesPackages("non-existing").size());

  }

  // FIXME this test doesn't work at all. The existing serialization DB somehow seems to work even with a broken implementation
  @Ignore
  @Test
  public void testDatabaseGetDependency() throws Exception {

    final PackageDatabase database = createDatabaseWithSampleData();

    final List<Package> packages = database.getDependency(database.getPackage("remote-connect"));

    assertNotNull(packages);
    assertEquals(1, packages.size());

    assertEquals("base", packages.get(0).getName());


    assertEquals(0, database.getDependency(database.getPackage("openthinclient-server-tftp")).size());

  }

  @Test
  public void testGetProvidedPackages() throws Exception {

    final PackageDatabase database = createDatabaseWithSampleData();

    final Map<String, org.openthinclient.util.dpkg.Package> providedPackages = database.getProvidedPackages();

    assertEquals(21, providedPackages.size());

    verifyPackage(providedPackages, "base", new Version("2.0-25"));
    verifyPackage(providedPackages, "cmdline", new Version("2.0-01"));
    verifyPackage(providedPackages, "cups-client", new Version("2.0-2"));
    verifyPackage(providedPackages, "desktop", new Version("2.0-15"));
    verifyPackage(providedPackages, "freerdp-git", new Version("2.0-1.2-12"));
    verifyPackage(providedPackages, "ica-client-12", new Version("2.0-12.1-13"));
    verifyPackage(providedPackages, "openthinclient-manager", new Version("1.0.2-1"));
    verifyPackage(providedPackages, "openthinclient-server-dhcp", new Version("1.0.0-1"));
    verifyPackage(providedPackages, "openthinclient-server-ldap", new Version("1.0.0-1"));
    verifyPackage(providedPackages, "openthinclient-server-nfs", new Version("1.0.0-1"));
    verifyPackage(providedPackages, "openthinclient-server-syslog", new Version("1.0.0-1"));
    verifyPackage(providedPackages, "openthinclient-server-tftp", new Version("1.0.0-1"));
    verifyPackage(providedPackages, "printserver", new Version("2.0-3"));
    verifyPackage(providedPackages, "rdesktop", new Version("2.0-1.8.2-4"));
    verifyPackage(providedPackages, "remote-connect", new Version("2.0-10"));
    verifyPackage(providedPackages, "smartcard-lite", new Version("2.0-2"));
    verifyPackage(providedPackages, "sso-tcos", new Version("2.0-4"));
    // FIXME the following lines would contain checks with the newest version but the serialization DB doesn't provide the lastest packages only
//    verifyPackage(providedPackages, "tcos-devices", new Version("2.0-20"));
//    verifyPackage(providedPackages, "tcos-libs", new Version("2.0-14"));
//    verifyPackage(providedPackages, "tcos-scripts", new Version("2.0-40"));

  }

  private void verifyPackage(Map<String, Package> providedPackages, String packageName, Version version) {

    final Package pkg = providedPackages.get(packageName);
    assertNotNull(pkg);

    assertEquals(packageName, pkg.getName());
    assertEquals(version, pkg.getVersion());

  }

  private PackageDatabase createDatabaseWithSampleData() throws IOException {
    final Path tempFile = createTemporaryFile();

    final PackageDatabase database = packageDatabaseFactory.create(tempFile);

    final DPKGPackageFactory dpkgPackageFactory = new DPKGPackageFactory();
    final List<org.openthinclient.util.dpkg.Package> packages = dpkgPackageFactory.getPackage(getClass().getResourceAsStream("/2015-05-15_manager-rolling-Packages.txt"));

    packages.forEach(database::addPackage);
    return database;
  }

  private Path createTemporaryFile() throws IOException {
    final Path tempFile = Files.createTempFile("otc-test-db", ".db");

    // the temporary fill will be created upon the call above.
    // As the old implementation expects no such file, when there is no existing database,
    // we're deleting the temporary file here.
    Files.delete(tempFile);
    return tempFile;
  }


  private DPKGPackage createSamplePackage(String name, String version) {
    final DPKGPackage pkg = new DPKGPackage();
    pkg.setName(name);
    pkg.setVersion(version);
    pkg.setProvides(new ANDReference(""));
    return pkg;
  }
}

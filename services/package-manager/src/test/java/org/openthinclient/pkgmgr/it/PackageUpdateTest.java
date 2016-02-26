package org.openthinclient.pkgmgr.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.DebianTestRepositoryServer;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.SimpleTargetDirectoryPackageManagerConfiguration;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.openthinclient.util.dpkg.Package;
import org.openthinclient.util.dpkg.Version;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PackageInstallTest.PackageManagerConfig.class)

public class PackageUpdateTest {

  private static DebianTestRepositoryServer testRepositoryServer;

  @Configuration()
  @Import(SimpleTargetDirectoryPackageManagerConfiguration.class)
  public static class PackageManagerConfig {

  }
  
  @BeforeClass
  public static void startRepoServer() {
    testRepositoryServer = new DebianTestRepositoryServer();
    testRepositoryServer.start();
  }
  
  @AfterClass
  public static void shutdownRepoServer() {
    testRepositoryServer.stop();
    testRepositoryServer = null;
  }
  
  PackageManagerConfiguration configuration;
  
  @Autowired
  ObjectFactory<PackageManagerConfiguration> packageManagerConfigurationObjectFactory;
  private DPKGPackageManager packageManager;

  @Before
  public void setupTestdir() throws Exception {
    configuration = packageManagerConfigurationObjectFactory.getObject();
    packageManager = preparePackageManager();
  }

  @After
  public void cleanup() throws Exception {
    Files.walkFileTree(configuration.getInstallDir().getParentFile().toPath(), new RecursiveDeleteFileVisitor());
  }
  
  @Test
  public void testUpdateSinglePackageFoo() throws Exception {
	  final List<Package> packages = packageManager.getInstallablePackages().stream()
			  .filter( pkg -> pkg.getName().equals("foo"))
			  .filter( pkg -> pkg.getVersion().equals(new Version("2.0-1")))
			  .collect(Collectors.<Package>toList());
	  assertContainsPackage(packages, "foo", "2.0-1");
	  
	  installPackages(packageManager, packages); 
 
	  assertVersion("foo", "2.0-1");	  
	  assertTrue("couldn't update foo-package", packageManager.update(packages));

	  final Path installDirectory = configuration.getInstallDir().toPath();	 	  
	  Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
	  for (Path file : pkgPath)
	    assertFileExists(file);
	  
	  assertVersion("foo", "2.1-1");
	  assertTestinstallDirectoryEmpty();
  }
  
  @Test 
  public void testUpdatePackageDependingOnNewerVersion() throws Exception {
	  final List<Package> packages = packageManager.getInstallablePackages().stream()
			  .filter( pkg -> pkg.getName().equals("foo") || pkg.getName().equals("bas"))
			  .filter( pkg -> pkg.getVersion().equals(new Version("2.0-1")))
			  .collect(Collectors.<Package>toList());
	  assertContainsPackage(packages, "foo", "2.0-1");
	  assertContainsPackage(packages, "bas", "2.0-1");
	  
	  installPackages(packageManager, packages);
	  
	  assertVersion("foo", "2.0-1");
	  assertVersion("bas", "2.0-1");
	  final List<Package> updateList = packages.stream().filter(
			    pkg -> pkg.getName().equals("bas")).collect(Collectors.<Package>toList());	
	  assertTrue("couldn't update bas-package", packageManager.update(updateList));

	  final Path installDirectory = configuration.getInstallDir().toPath();
	  Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
	  for (Path file : pkgPath)
	    assertFileExists(file);
	  pkgPath = getFilePathsInPackage("bas", installDirectory);
	  for (Path file : pkgPath)
	    assertFileExists(file);

	  assertVersion("foo", "2.1-1");
	  assertVersion("bas", "2.1-1");
	  assertTestinstallDirectoryEmpty();
  }
  
  @Test
  public void testUpdatePackageDependingOnNotInstalledPackage() throws Exception {
	  final List<Package> packages = packageManager.getInstallablePackages().stream()
			  .filter( pkg -> pkg.getName().equals("zonk"))
			  .filter( pkg -> pkg.getVersion().equals(new Version("2.0-1")))
			  .collect(Collectors.<Package>toList());
	  assertContainsPackage(packages, "zonk", "2.0-1");
	  
	  installPackages(packageManager, packages);
	  
	  assertVersion("zonk", "2.0-1");
	  assertTrue("couldn't update zonk-package", packageManager.update(packages));
	  
	  final Path installDirectory = configuration.getInstallDir().toPath();
	  Path[] pkgPath = getFilePathsInPackage("zonk", installDirectory);
	  for (Path file : pkgPath)
	    assertFileExists(file);
	  pkgPath = getFilePathsInPackage("foo", installDirectory);
	  for (Path file : pkgPath)
	    assertFileExists(file);

	  assertVersion("zonk", "2.1-1");
	  assertTestinstallDirectoryEmpty();
  }
  
  @Test
  public void testUpdatePackageReplacingOtherPackage() throws Exception {
	  final List<Package> packages = packageManager.getInstallablePackages().stream()
			  .filter( pkg -> pkg.getName().equals("foo") || pkg.getName().equals("bar2"))
			  .filter( pkg -> pkg.getVersion().equals(new Version("2.0-1")))
			  .collect(Collectors.<Package>toList());
	  assertContainsPackage(packages, "foo", "2.0-1");
	  assertContainsPackage(packages, "bar2", "2.0-1");
	  
	  installPackages(packageManager, packages);

	  assertVersion("bar2", "2.0-1");
	  final List<Package> updateList = packages.stream().filter(
			    pkg -> pkg.getName().equals("bar2")).collect(Collectors.<Package>toList());	  
	  assertTrue("couldn't update bar2-package", packageManager.update(updateList));

	  final Path installDirectory = configuration.getInstallDir().toPath();  
	  Path[] pkgPath = getFilePathsInPackage("bar2", installDirectory);
	  for (Path file : pkgPath)
	    assertFileExists(file);
	  pkgPath = getFilePathsInPackage("foo", installDirectory);
	  for (Path file : pkgPath)
	    assertFileNotExists(file);

	  assertVersion("bar2", "2.1-1");
	  assertTestinstallDirectoryEmpty();
  }
  
  private void assertFileExists(Path path) {
	    assertTrue(path.getFileName() + " does not exist", Files.exists(path));
	    assertTrue(path.getFileName() + " is not a regular file", Files.isRegularFile(path));
  }
  
  private void assertFileNotExists(Path path) {
	    assertTrue(path.getFileName() + " does exist", !Files.exists(path));
  }
  
  private void assertTestinstallDirectoryEmpty() throws IOException {
	  final Path testInstallDirectory = configuration.getTestinstallDir().toPath();
	  assertEquals("test-install-directory isn't empty", 0, Files.list(testInstallDirectory).count());
  }

  private void assertContainsPackage(final List<Package> packages, final String packageName, final String version) {
	  assertTrue("missing " + packageName + " package (Version: " + version + " )",
			  packages.stream()
			  .filter(p -> p.getName().equals(packageName))
			  .filter(p -> p.getVersion().equals(new Version(version)))
			  .findFirst().isPresent());
  }
  
  private void assertVersion(final String pkg, final String version) throws Exception {
	  final Path installDirectory = configuration.getInstallDir().toPath();
	  final Path versionFile = installDirectory.resolve("version").resolve(pkg + "version.txt");
	  assertFileExists(versionFile);
	  try(final BufferedReader in = new BufferedReader(new FileReader(versionFile.toFile()))) {
		  assertEquals("wrong package version", version, in.readLine().trim());
		  in.close();
	  }
  }

  private void installPackages(DPKGPackageManager packageManager, List<Package> packages) throws Exception {

	assertTrue("couldn't install required packages", packageManager.install(packages));
	final Path installDirectory = configuration.getInstallDir().toPath();

	for(Package pkg : packages ) {
			String pkgName = pkg.getName();
	        Path[] pkgPath = getFilePathsInPackage(pkgName, installDirectory);
	        for (Path file : pkgPath)
	          assertFileExists(file);
	}
	assertTestinstallDirectoryEmpty();
  }

  private DPKGPackageManager preparePackageManager() throws Exception {
	  final DPKGPackageManager packageManager = PackageManagerFactory.createPackageManager(configuration);
	
	  writeSourcesList();
	
	  assertNotNull(packageManager.getSourcesList());
	  assertEquals(1, packageManager.getSourcesList().getSources().size());
	  assertEquals(testRepositoryServer.getServerUrl(), packageManager.getSourcesList().getSources().get(0).getUrl());

	  assertEquals(0, packageManager.getInstallablePackages().size());
	  assertTrue(packageManager.updateCacheDB());
	  assertEquals(12, packageManager.getInstallablePackages().size());
	  
	  return packageManager;
  }
  
  private void writeSourcesList() throws Exception {
	  try (final FileOutputStream out = new FileOutputStream(configuration.getSourcesList())) {
		  out.write(("deb " + testRepositoryServer.getServerUrl().toExternalForm() + " ./").getBytes());
	  }
  }

  private Path[] getFilePathsInPackage(String pkg, Path directory) {
    Path[] filePaths = new Path[3];
    filePaths[0] = directory.resolve("schema").resolve("application").resolve(pkg + ".xml");
    filePaths[1] = directory.resolve("schema").resolve("application").resolve(pkg + "-tiny.xml.sample");
    filePaths[2] = directory.resolve("sfs").resolve("package").resolve(pkg + ".sfs");
    return filePaths;
  }

}



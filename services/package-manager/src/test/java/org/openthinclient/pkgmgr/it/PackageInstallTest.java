package org.openthinclient.pkgmgr.it;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PackageInstallTest.PackageManagerConfig.class)
public class PackageInstallTest {

  private static DebianTestRepositoryServer testRepositoryServer;

  @Configuration()
  @Import(SimpleTargetDirectoryPackageManagerConfiguration.class)
  public static class PackageManagerConfig {}

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
    Files.walkFileTree(configuration.getInstallDir().getParentFile().toPath(),
        new RecursiveDeleteFileVisitor());
  }

  @Test
  public void testInstallSinglePackageFoo() throws Exception {

    final List<Package> packages =
        packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("foo"))
            .filter(pkg -> pkg.getVersion().equals(new Version("2.0-1")))
            .collect(Collectors.<Package>toList());

    assertContainsPackage(packages, "foo", "2.0-1");
    assertTrue("couldn't install foo-package", packageManager.install(packages));

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);
    assertTestinstallDirectoryEmpty();
  }

  @Test
  public void testInstallFooAndZonkPackages() throws Exception {

    final List<Package> packages = packageManager.getInstallablePackages().stream()
        .filter(pkg -> pkg.getName().equals("foo") || pkg.getName().equals("zonk"))
        .filter(pkg -> pkg.getVersion().equals(new Version("2.0-1")))
        .collect(Collectors.<Package>toList());

    assertContainsPackage(packages, "foo", "2.0-1");
    assertContainsPackage(packages, "zonk", "2.0-1");

    assertTrue("couldn't install foo and zonk-packages", packageManager.install(packages));

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    pkgPath = getFilePathsInPackage("zonk", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  @Test
  public void testInstallBar2WithFooExisting() throws Exception {

    testInstallSinglePackageFoo();

    final List<Package> packages =
        packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("bar2"))
            .filter(pkg -> pkg.getVersion().equals(new Version("2.0-1")))
            .collect(Collectors.<Package>toList());

    assertContainsPackage(packages, "bar2", "2.0-1");

    assertTrue("couldn't install bar2-package", packageManager.install(packages));

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("bar2", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  @Test
  public void testInstallBar2WithFooNotExisting() throws Exception {

    final List<Package> packages =
        packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("bar2"))
            .filter(pkg -> pkg.getVersion().equals(new Version("2.0-1")))
            .collect(Collectors.<Package>toList());

    assertContainsPackage(packages, "bar2", "2.0-1");

    assertFalse("installation of bar2 package didn't fail", packageManager.install(packages));

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);
    pkgPath = getFilePathsInPackage("bar2", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  @Test
  public void testInstallBarWithFooNotExisting() throws Exception {

    final List<Package> packages =
        packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("bar"))
            .filter(pkg -> pkg.getVersion().equals(new Version("2.0-1")))
            .collect(Collectors.<Package>toList());

    assertContainsPackage(packages, "bar", "2.0-1");

    assertFalse("installation of bar package didn't fail", packageManager.install(packages));

    assertInstallDirectoryEmpty();
    assertTestinstallDirectoryEmpty();
  }

  @Test
  public void testInstallBarWithFooExisitingInOlderVersion() throws Exception {

    testInstallSinglePackageFoo();

    final List<Package> packages =
        packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("bar"))
            .filter(pkg -> pkg.getVersion().equals(new Version("2.0-1")))
            .collect(Collectors.<Package>toList());

    assertContainsPackage(packages, "bar", "2.0-1");

    assertTrue("couldn't install bar-package", packageManager.install(packages));

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);
    pkgPath = getFilePathsInPackage("bar", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  @Test
  public void testInstallFooAndZonkAndBar2() throws Exception {

    final List<Package> packages = packageManager.getInstallablePackages().stream()
        .filter(pkg -> pkg.getName().equals("foo") || pkg.getName().equals("zonk")
            || pkg.getName().equals("bar2"))
        .filter(pkg -> pkg.getVersion().equals(new Version("2.0-1")))
        .collect(Collectors.<Package>toList());

    assertContainsPackage(packages, "foo", "2.0-1");
    assertContainsPackage(packages, "zonk", "2.0-1");
    assertContainsPackage(packages, "bar2", "2.0-1");

    assertTrue("couldn't install foo, zonk and bar2-packages", packageManager.install(packages));

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    pkgPath = getFilePathsInPackage("zonk", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    pkgPath = getFilePathsInPackage("bar2", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }
  
  @Test
  public void testCircularDependency() throws Exception {
    
    final List<Package> packages = packageManager.getInstallablePackages().stream()
        .filter(pkg -> pkg.getName().equals("bas2") || pkg.getName().equals("zonk2"))
        .filter(pkg -> pkg.getVersion().equals(new Version("2.0-1")))
        .collect(Collectors.<Package>toList());
    
    assertContainsPackage(packages, "bas2", "2.0-1");
    assertContainsPackage(packages, "zonk2", "2.0-1");
    
    assertFalse("didn't reject circular depending packages bas2 and zonk2", packageManager.install(packages));
    
    assertInstallDirectoryEmpty();
    assertTestinstallDirectoryEmpty();
  }
  
  @Test
  public void testInstallBar2Dev() throws Exception  {
    
    testInstallSinglePackageFoo();

    final List<Package> packages = packageManager.getInstallablePackages().stream()
        .filter(pkg -> pkg.getName().equals("bar2") || pkg.getName().equals("bar2-dev "))
        .filter(pkg -> pkg.getVersion().equals(new Version("2.0-1")))
        .collect(Collectors.<Package>toList());

    assertContainsPackage(packages, "bar2", "2.0-1");
    assertContainsPackage(packages, "bar2-dev", "2.0-1");
    
    assertTrue("couldn't install bar2 and bar2-dev-package", packageManager.install(packages));

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);
    pkgPath = getFilePathsInPackage("bar2", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);
    pkgPath = getFilePathsInPackage("bar2-dev", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  @Test
  public void testInstallBasDev() throws Exception  {

    final List<Package> packages = packageManager.getInstallablePackages().stream()
        .filter(pkg -> pkg.getName().equals("foo") || pkg.getName().equals("bas-dev "))
        .filter(pkg -> pkg.getVersion().equals(new Version("2.0-1")))
        .collect(Collectors.<Package>toList());

    assertContainsPackage(packages, "foo", "2.0-1");
    assertContainsPackage(packages, "bas-dev", "2.0-1");
    
    assertTrue("couldn't install bas-dev and foo", packageManager.install(packages));

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    for (Path file : pkgPath)
      assertFileNotExists(file);
    pkgPath = getFilePathsInPackage("bas-dev", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  @Test 
  public void testInstallZonkDev() throws Exception  {
    
    testInstallSinglePackageFoo();

    final List<Package> packages = packageManager.getInstallablePackages().stream()
        .filter(pkg -> pkg.getName().equals("bar2") || pkg.getName().equals("zonk-dev "))
        .filter(pkg -> pkg.getVersion().equals(new Version("2.0-1")))
        .collect(Collectors.<Package>toList());

    assertContainsPackage(packages, "bar2", "2.0-1");
    assertContainsPackage(packages, "zonk-dev", "2.0-1");
    
    assertTrue("couldn't install bar2 and zonk-dev-package", packageManager.install(packages));

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    for (Path file : pkgPath)
      assertFileNotExists(file);
    pkgPath = getFilePathsInPackage("bar2", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);
    pkgPath = getFilePathsInPackage("zonk-dev", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  private void assertTestinstallDirectoryEmpty() throws IOException {
    final Path testInstallDirectory = configuration.getTestinstallDir().toPath();
    assertEquals("test-install-directory isn't empty", 0, Files.list(testInstallDirectory).count());
  }

  private void assertInstallDirectoryEmpty() throws IOException {
    final Path installDirectory = configuration.getInstallDir().toPath();
    assertEquals("install-directory isn't empty", 0, Files.list(installDirectory).count());
  }

  private void assertContainsPackage(final List<Package> packages, final String packageName,
      final String version) {
    assertTrue("missing " + packageName + " package (Version: " + version + " )",
        packages.stream().filter(p -> p.getName().equals(packageName))
            .filter(p -> p.getVersion().equals(new Version(version))).findFirst().isPresent());
  }

  private DPKGPackageManager preparePackageManager() throws Exception {
    final DPKGPackageManager packageManager =
        PackageManagerFactory.createPackageManager(configuration);


    writeSourcesList();

    assertNotNull("failed to create package manager instance", packageManager);
    assertNotNull("sources-list could not be loaded", packageManager.getSourcesList());
    assertEquals("number of entries in sources list is not correct", 1,
        packageManager.getSourcesList().getSources().size());
    assertEquals("wrong URL of repository", testRepositoryServer.getServerUrl(),
        packageManager.getSourcesList().getSources().get(0).getUrl());

    assertEquals(0, packageManager.getInstallablePackages().size());
    assertTrue("couldn't update cache-DB", packageManager.updateCacheDB());
    assertEquals("wrong number of installables packages", 12,
        packageManager.getInstallablePackages().size());

    saveDB();

    return packageManager;
  }

  private void writeSourcesList() throws Exception {

    try (final FileOutputStream out = new FileOutputStream(configuration.getSourcesList())) {
      out.write(("deb " + testRepositoryServer.getServerUrl().toExternalForm() + " ./").getBytes());
    }
  }

  private void saveDB() throws Exception {
    File dbDir = configuration.getPackageDB().getParentFile();
    for (File dbFile : dbDir.listFiles()) {
      File outFile = new File(dbFile.getPath() + "-save");
      FileInputStream is = new FileInputStream(dbFile);
      FileOutputStream os = new FileOutputStream(outFile);
      while (is.available() != 0) {
        os.write(is.read());
      }
      is.close();
      os.close();
    }
  }

  private void assertFileExists(Path path) {
    assertTrue(path.getFileName() + " does not exist", Files.exists(path));
    assertTrue(path.getFileName() + " is not a regular file", Files.isRegularFile(path));
  }

  private void assertFileNotExists(Path path) {
    assertFalse(path.getFileName() + " does exist", Files.exists(path));
  }

  private Path[] getFilePathsInPackage(String pkg, Path directory) {
    Path[] filePaths = new Path[3];
    filePaths[0] = directory.resolve("schema").resolve("application").resolve(pkg + ".xml");
    filePaths[1] =
        directory.resolve("schema").resolve("application").resolve(pkg + "-tiny.xml.sample");
    filePaths[2] = directory.resolve("sfs").resolve("package").resolve(pkg + ".sfs");
    return filePaths;
  }
}

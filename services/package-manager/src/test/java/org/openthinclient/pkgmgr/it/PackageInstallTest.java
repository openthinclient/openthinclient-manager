package org.openthinclient.pkgmgr.it;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.DebianTestRepositoryServer;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.PackageTestUtils;
import org.openthinclient.pkgmgr.SimpleTargetDirectoryPackageManagerConfiguration;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.progress.ListenableProgressFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.openthinclient.pkgmgr.PackageTestUtils.getFilePathsInPackage;
import static org.openthinclient.pkgmgr.PackagesUtil.PACKAGES_SIZE;
import static org.openthinclient.pkgmgr.it.PackageManagerTestUtils.doInstallPackages;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PackageInstallTest.PackageManagerConfig.class)
@Sql(executionPhase=ExecutionPhase.AFTER_TEST_METHOD,  scripts="classpath:sql/empty-tables.sql")
public class PackageInstallTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(PackageInstallTest.class);
  
  @ClassRule
  public static final DebianTestRepositoryServer testRepositoryServer = new DebianTestRepositoryServer();

  PackageManagerConfiguration configuration;

  @Autowired
  ObjectFactory<PackageManagerConfiguration> packageManagerConfigurationObjectFactory;
  @Autowired
  PackageManagerFactory packageManagerFactory;

  private PackageManager packageManager;
  
  
  
  @Before
  public void setupTestdir() throws Exception {
    configuration = packageManagerConfigurationObjectFactory.getObject();
    packageManager = preparePackageManager();
  }
  

  @After
  public void cleanup() throws Exception {
    // TODO jn: FileWalker causes 'directory not empty' on delete at WINDOWS boxes
    Files.walkFileTree(configuration.getInstallDir().getParentFile().toPath(), new RecursiveDeleteFileVisitor());
  }

  @Test
  public void testInstallSinglePackageFoo() throws Exception {

    final List<Package> packages =
        packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("foo"))
                .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                .collect(Collectors.toList());

    assertContainsPackage(packages, "foo", "2.0-1");
    doInstallPackages(packageManager, packages);

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
            .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
            .collect(Collectors.toList());

    assertContainsPackage(packages, "foo", "2.0-1");
    assertContainsPackage(packages, "zonk", "2.0-1");

    doInstallPackages(packageManager, packages);

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    pkgPath = getFilePathsInPackage("zonk", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  private void assertContainsPackage(final List<Package> packages, final String packageName) {
    assertTrue("missing " + packageName + " package",
            packages.stream().filter(p -> p.getName().equals(packageName)).findFirst().isPresent());
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
                    .filter(p -> p.getVersion().equals(Version.parse(version))).findFirst().isPresent());
  }

  private void assertPackageInstallationWithUserInteraction() {
    fail("user interactions not yet implemented");
  }

  private void assertFileNotExists(Path path) {
    assertFalse(path.getFileName() + " does exist", Files.exists(path));
  }

  @Test
  public void testInstallBar2WithFooExisting() throws Exception {

    testInstallSinglePackageFoo();

    final List<Package> packages =
        packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("bar2"))
                .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                .collect(Collectors.toList());

    assertContainsPackage(packages, "bar2", "2.0-1");

    doInstallPackages(packageManager, packages);

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
                .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                .collect(Collectors.toList());

    assertContainsPackage(packages, "bar2", "2.0-1");

    doInstallPackages(packageManager, packages);

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
  public void testInstallBarWithFooExisitingInOlderVersion() throws Exception {

    testInstallSinglePackageFoo();

    final List<Package> packages =
        packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("bar"))
                .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                .collect(Collectors.toList());

    assertContainsPackage(packages, "bar", "2.0-1");

    doInstallPackages(packageManager, packages);

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
  public void testProvidingMultipleFooUserDecisionChoosingFoo() throws Exception {

    final List<Package> packages =
            packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("bar2"))
                    .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                    .collect(Collectors.toList());

    assertContainsPackage(packages, "bar2", "2.0-1");

    // TODO: implement PackageInstallationWithUserInteraction 
    // assertPackageInstallationWithUserInteraction(); // choose installation of foo
    //
    //    final Path installDirectory = configuration.getInstallDir().toPath();
    //
    //    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    //    for (Path file : pkgPath)
    //      assertFileExists(file);
    //    pkgPath = getFilePathsInPackage("bar2", installDirectory);
    //    for (Path file : pkgPath)
    //      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  @Test
  public void testProvidingMultipleFooUserDecisionChoosingFooFork() throws Exception {

    final List<Package> packages =
            packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("bar2"))
                    .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                    .collect(Collectors.toList());

    assertContainsPackage(packages, "bar2", "2.0-1");
    
    // TODO: implement PackageInstallationWithUserInteraction 
    // assertPackageInstallationWithUserInteraction(); // choose installation of foo
    //
    //    final Path installDirectory = configuration.getInstallDir().toPath();
    //
    //    Path[] pkgPath = getFilePathsInPackage("foo-fork", installDirectory);
    //    for (Path file : pkgPath)
    //      assertFileExists(file);
    //    pkgPath = getFilePathsInPackage("bar2", installDirectory);
    //    for (Path file : pkgPath)
    //      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  @Test
  public void testProvidingMultipleFooConflictingDecision() throws Exception {

    List<Package> packages =
            packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("zonk"))
                    .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                    .collect(Collectors.toList());

    assertContainsPackage(packages, "zonk", "2.0-1");
    doInstallPackages(packageManager, packages);

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("zonk", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);
    assertTestinstallDirectoryEmpty();

    packages =
            packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("bar2"))
                    .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                    .collect(Collectors.toList());

    assertContainsPackage(packages, "bar2", "2.0-1");
    doInstallPackages(packageManager, packages);

    pkgPath = getFilePathsInPackage("foo", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);
    pkgPath = getFilePathsInPackage("bar2", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);
    pkgPath = getFilePathsInPackage("zonk", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  @Test
  public void testInstallBar2WithZonkDevProvidingFoo() throws Exception {

    List<Package> packages =
            packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("zonk-dev"))
                    .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                    .collect(Collectors.toList());

    assertContainsPackage(packages, "zonk-dev", "2.0-1");
    doInstallPackages(packageManager, packages);

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("zonk-dev", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);
    assertTestinstallDirectoryEmpty();

    packages =
            packageManager.getInstallablePackages().stream().filter(pkg -> pkg.getName().equals("bar2"))
                    .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                    .collect(Collectors.toList());

    assertContainsPackage(packages, "bar2", "2.0-1");

    doInstallPackages(packageManager, packages);

    pkgPath = getFilePathsInPackage("zonk-dev", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);
    pkgPath = getFilePathsInPackage("bar2", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  @Test
  public void testInstallFooSuggestingBas() throws Exception {

    final List<Package> packages = new ArrayList<>();
    packages.addAll(packageManager.getInstallablePackages().stream()
            .filter(pkg -> pkg.getName().equals("foo"))
            .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
            .collect(Collectors.toList()));

    assertContainsPackage(packages, "foo", "2.0-1");
    
    
    // TODO: implement PackageInstallationWithUserInteraction 
    // assertPackageInstallationWithUserInteraction(); // pkgmanager suggests to install bas, choose installation of bar
    //
    //    final Path installDirectory = configuration.getInstallDir().toPath();
    //
    //    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    //    for (Path file : pkgPath)
    //      assertFileExists(file);
    //
    //    pkgPath = getFilePathsInPackage("bas", installDirectory);
    //    for (Path file : pkgPath)
    //      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }
  private PackageManager preparePackageManager() throws Exception {
    final PackageManager packageManager = packageManagerFactory.createPackageManager(configuration);

    assertNotNull("failed to create package manager instance", packageManager);

    PackageTestUtils.configureSources(testRepositoryServer, packageManager);

    assertNotNull("sources-list could not be loaded", packageManager.getSourcesList());
    assertEquals("number of entries in sources list is not correct", 1, packageManager.getSourcesList().getSources().size());
    assertEquals("wrong URL of repository", testRepositoryServer.getServerUrl(), packageManager.getSourcesList().getSources().get(0).getUrl());

    //assertEquals(0, packageManager.findByInstalledFalse().size());
    final ListenableProgressFuture<PackageListUpdateReport> updateFuture = packageManager.updateCacheDB();

    assertNotNull("couldn't update cache-DB", updateFuture.get());
    assertEquals("wrong number of installables packages", PACKAGES_SIZE, packageManager.getInstallablePackages().size());

    return packageManager;
  }

  private void assertFileExists(Path path) {
    assertTrue(path.getFileName() + " does not exist", Files.exists(path));
    assertTrue(path.getFileName() + " is not a regular file", Files.isRegularFile(path));
  }

  @Configuration()
  @Import({SimpleTargetDirectoryPackageManagerConfiguration.class})
  public static class PackageManagerConfig {

  }
}

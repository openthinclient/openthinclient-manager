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
import org.openthinclient.pkgmgr.SimpleTargetDirectoryPackageManagerConfiguration;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = PackageInstallTest.PackageManagerConfig.class)
public class PackageInstallTest {

  @ClassRule
  public static final DebianTestRepositoryServer testRepositoryServer = new DebianTestRepositoryServer();

  PackageManagerConfiguration configuration;
  @Autowired
  ObjectFactory<PackageManagerConfiguration> packageManagerConfigurationObjectFactory;
  @Autowired
  SourceRepository sourceRepository;
  @Autowired
  PackageRepository packageRepository;
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
    Files.walkFileTree(configuration.getInstallDir().getParentFile().toPath(), new RecursiveDeleteFileVisitor());
  }

  @Test
  public void testInstallSinglePackageFoo() throws Exception {

    final Optional<Package> testPackage = packageManager.getInstallablePackages().stream().filter(
        pkg -> pkg.getName().equals("foo")).findFirst();

    assertTrue("foo-Package wasn't avaible", testPackage.isPresent());
    assertTrue("couldn't install foo-package", executeInstall(Collections.singletonList(testPackage.get())));

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);
    assertTestinstallDirectoryEmpty();

  }

  @Test
  public void testInstallFooAndZonkPackages() throws Exception {


    final List<Package> packages = packageManager.getInstallablePackages().stream().filter(
        pkg -> pkg.getName().equals("foo") || pkg.getName().equals("zonk")).collect(Collectors.<Package>toList());

    assertContainsPackage(packages, "foo");
    assertContainsPackage(packages, "zonk");

    assertTrue("couldn't install foo and zonk-packages", executeInstall(packages));

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    pkgPath = getFilePathsInPackage("zonk", installDirectory);
    for (Path file : pkgPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();

  }

  private void assertTestinstallDirectoryEmpty() throws IOException {
    final Path testInstallDirectory = configuration.getTestinstallDir().toPath();
    assertEquals("test-install-directory isn't empty", 0, Files.list(testInstallDirectory).count());
  }

  private void assertContainsPackage(final List<Package> packages, final String packageName) {
    assertTrue("missing " + packageName + " package",
        packages.stream().filter(p -> p.getName().equals(packageName)).findFirst().isPresent());
  }

  @Test
  public void testInstallSinglePackageDependingOther() throws Exception {

    final Optional<org.openthinclient.pkgmgr.db.Package> bar2Package = packageManager.getInstallablePackages().stream().filter(
        pkg -> pkg.getName().equals("bar2")).findFirst();

    assertTrue(bar2Package.isPresent());
    assertTrue(executeInstall(Collections.singletonList(bar2Package.get())));

    final Path installDirectory = configuration.getInstallDir().toPath();

    Path[] fooPath = getFilePathsInPackage("foo", installDirectory);
    Path[] barPath = getFilePathsInPackage("bar2", installDirectory);
    for (Path file : fooPath)
      assertFileExists(file);
    for (Path file : barPath)
      assertFileExists(file);

    assertTestinstallDirectoryEmpty();
  }

  private boolean executeInstall(List<Package> packages) {
    // FIXME
    fail("Missing install implementation right now");
    return false;
  }

  private PackageManager preparePackageManager() throws Exception {
    configureSources(sourceRepository);
    final PackageManager packageManager = packageManagerFactory.createPackageManager(configuration);

    assertNotNull("failed to create package manager instance", packageManager);
    assertNotNull("sources-list could not be loaded", packageManager.getSourcesList());
    assertEquals("number of entries in sources list is not correct", 1,
        packageManager.getSourcesList().getSources().size());
    assertEquals("wrong URL of repository", testRepositoryServer.getServerUrl(),
        packageManager.getSourcesList().getSources().get(0).getUrl());

    //assertEquals(0, packageManager.findByInstalledFalse().size());
    final ListenableProgressFuture<PackageListUpdateReport> updateFuture = packageManager.updateCacheDB();

    assertNotNull("couldn't update cache-DB", updateFuture.get());
    assertEquals("wrong number of installables packages", 4, packageManager.getInstallablePackages().size());

    return packageManager;
  }

  private void configureSources(SourceRepository repository) throws Exception {

    repository.deleteAll();

    final Source source = new Source();
    source.setEnabled(true);
    source.setUrl(testRepositoryServer.getServerUrl());

    repository.save(source);
  }

  private void assertFileExists(Path path) {
    assertTrue(path.getFileName() + " does not exist", Files.exists(path));
    assertTrue(path.getFileName() + " is not a regular file", Files.isRegularFile(path));
  }

  private Path[] getFilePathsInPackage(String pkg, Path directory) {
    Path[] filePaths = new Path[3];
    filePaths[0] = directory.resolve("schema").resolve("application").resolve(pkg + ".xml");
    filePaths[1] = directory.resolve("schema").resolve("application").resolve(pkg + "-tiny.xml.sample");
    filePaths[2] = directory.resolve("sfs").resolve("package").resolve(pkg + ".sfs");
    return filePaths;
  }

  @Configuration()
  @Import({SimpleTargetDirectoryPackageManagerConfiguration.class})
  public static class PackageManagerConfig {

  }
}

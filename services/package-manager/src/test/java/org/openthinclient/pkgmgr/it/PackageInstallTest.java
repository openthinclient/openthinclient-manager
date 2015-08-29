package org.openthinclient.pkgmgr.it;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.DebianTestRepositoryServer;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.SimpleTargetDirectoryPackageManagerConfiguration;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PackageInstallTest.PackageManagerConfig.class)
public class PackageInstallTest {

  private static DebianTestRepositoryServer testRepositoryServer;
  @Autowired
  PackageManagerConfiguration configuration;

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

  @Test
  public void testInstallSinglePackage() throws Exception {
    final DPKGPackageManager packageManager = preparePackageManager();

    final Optional<org.openthinclient.util.dpkg.Package> fooPackage = packageManager.getInstallablePackages()
            .stream()
            .filter(pkg -> pkg.getName().equals("foo"))
            .findFirst();

    assertTrue(fooPackage.isPresent());
    assertTrue(packageManager.install(Collections.singletonList(fooPackage.get())));

    // verify the existence files from the package.
  }

  private DPKGPackageManager preparePackageManager() throws Exception {
    final DPKGPackageManager packageManager = PackageManagerFactory.createPackageManager(configuration);

    writeSourcesList();

    assertNotNull(packageManager.getSourcesList());
    assertEquals(1, packageManager.getSourcesList().getSources().size());
    assertEquals(testRepositoryServer.getServerUrl(), packageManager.getSourcesList().getSources().get(0).getUrl());

    assertEquals(0, packageManager.getInstallablePackages().size());
    assertTrue(packageManager.updateCacheDB());
    assertEquals(4, packageManager.getInstallablePackages().size());

    return packageManager;
  }

  private void writeSourcesList() throws Exception {

    try (final FileOutputStream out = new FileOutputStream(configuration.getSourcesList())) {
      out.write(("deb " + testRepositoryServer.getServerUrl().toExternalForm() + " ./").getBytes());
    }
  }

  @Configuration()
  @Import(SimpleTargetDirectoryPackageManagerConfiguration.class)
  public static class PackageManagerConfig {

  }
}

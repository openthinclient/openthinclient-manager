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
import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.Arrays;

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

    File fooXml = new File(configuration.getInstallDir().getPath() + File.separator + "schema" + File.separator + "application" + File.separator + "foo.xml");
    File fooSample = new File(configuration.getInstallDir().getPath() + File.separator + "schema" + File.separator + "application" + File.separator + "foo-tiny.xml.sample");
    File fooSfs = new File(configuration.getInstallDir().getPath() + File.separator + "sfs" + File.separator + "package" + File.separator + "foo.sfs");

    assertTrue("foo.xml not installed",fooXml.exists());
    assertTrue("foo-tiny.xml.sample not installed",fooSample.exists());
    assertTrue("foo.sfs not installed",fooSfs.exists());
    
    assertEquals(0,configuration.getTestinstallDir().listFiles().length);
  }
  
  @Test
  public void testInstallSinglePackageDependingOther() throws Exception {
    final DPKGPackageManager packageManager = preparePackageManager();

    final Optional<org.openthinclient.util.dpkg.Package> bar2Package = packageManager.getInstallablePackages()
            .stream()
            .filter(pkg -> pkg.getName().equals("bar2"))
            .findFirst();

    assertTrue(bar2Package.isPresent());
    assertTrue(packageManager.install(Collections.singletonList(bar2Package.get())));

    File fooXml = new File(configuration.getInstallDir().getPath() + File.separator + "schema" + File.separator + "application" + File.separator + "foo.xml");
    File fooSample = new File(configuration.getInstallDir().getPath() + File.separator + "schema" + File.separator + "application" + File.separator + "foo-tiny.xml.sample");
    File fooSfs = new File(configuration.getInstallDir().getPath() + File.separator + "sfs" + File.separator + "package" + File.separator + "foo.sfs");
    File bar2Xml = new File(configuration.getInstallDir().getPath() + File.separator + "schema" + File.separator + "application" + File.separator + "bar2.xml");
    File bar2Sample = new File(configuration.getInstallDir().getPath() + File.separator + "schema" + File.separator + "application" + File.separator + "bar2-tiny.xml.sample");
    File bar2Sfs = new File(configuration.getInstallDir().getPath() + File.separator + "sfs" + File.separator + "package" + File.separator + "bar2.sfs");

    assertTrue("foo.xml not installed",fooXml.exists());
    assertTrue("foo-tiny.xml.sample not installed",fooSample.exists());
    assertTrue("foo.sfs not installed",fooSfs.exists());
    assertTrue("bar2.xml not installed",bar2Xml.exists());
    assertTrue("bar2-tiny.xml.sample not installed",bar2Sample.exists());
    assertTrue("bar2.sfs not installed",bar2Sfs.exists());
    
    assertEquals(0,configuration.getTestinstallDir().listFiles().length);
  }

  private DPKGPackageManager preparePackageManager() throws Exception {
    final DPKGPackageManager packageManager = PackageManagerFactory.createPackageManager(configuration);

    writeSourcesList();

    assertNotNull(packageManager.getSourcesList());
    assertEquals(1, packageManager.getSourcesList().getSources().size());
    assertEquals(testRepositoryServer.getServerUrl(), packageManager.getSourcesList().getSources().get(0).getUrl());

    //assertEquals(0, packageManager.getInstallablePackages().size());
    assertTrue(packageManager.updateCacheDB());
    //assertEquals(4, packageManager.getInstallablePackages().size());

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

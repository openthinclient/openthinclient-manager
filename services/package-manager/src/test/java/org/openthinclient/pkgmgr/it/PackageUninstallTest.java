package org.openthinclient.pkgmgr.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PackageInstallTest.PackageManagerConfig.class)

public class PackageUninstallTest {

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
  public void testUninstallSinglePackage() throws Exception {
//	  final DPKGPackageManager packageManager = preparePackageManager();
//	  
//	  installPackages(packageManager);
  }
  
  private void installPackages(DPKGPackageManager packageManager) throws Exception {
	    Collection<org.openthinclient.util.dpkg.Package> installables = packageManager.getInstallablePackages();
	    
	    assertTrue(!installables.isEmpty());
	    assertTrue(packageManager.install(installables));
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



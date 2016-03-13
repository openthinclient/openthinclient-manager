package org.openthinclient.pkgmgr.it;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.DebianTestRepositoryServer;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = PackageInstallTest.PackageManagerConfig.class)
public class PackageUninstallTest {

  private static DebianTestRepositoryServer testRepositoryServer;
    @Autowired
    PackageManagerConfiguration configuration;
    @Autowired
    SourceRepository sourceRepository;
    @Autowired
    PackageRepository packageRepository;

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
	  final DPKGPackageManager packageManager = preparePackageManager();
	  
	  installPackages(packageManager);
  }

  private void installPackages(DPKGPackageManager packageManager) throws Exception {

      fail("missing implementation right now");

//    Collection<org.openthinclient.pkgmgr.db.Package> installables = packageManager.getInstallablePackages();
//
//    assertTrue(!installables.isEmpty());
//	    assertTrue(packageManager.install(installables));
  }

  private DPKGPackageManager preparePackageManager() throws Exception {
    configureSources(sourceRepository);
      final DPKGPackageManager packageManager = PackageManagerFactory.createPackageManager(configuration, sourceRepository, packageRepository, installationRepository, installationLogEntryRepository);


      assertNotNull(packageManager.getSourcesList());
	  assertEquals(1, packageManager.getSourcesList().getSources().size());
	  assertEquals(testRepositoryServer.getServerUrl(), packageManager.getSourcesList().getSources().get(0).getUrl());

	  assertEquals(0, packageManager.getInstallablePackages().size());
	  assertTrue(packageManager.updateCacheDB());
	  assertEquals(4, packageManager.getInstallablePackages().size());
	  
	  return packageManager;
}
  private void configureSources(SourceRepository repository) throws Exception {

    repository.deleteAll();

    final Source source = new Source();
    source.setEnabled(true);
    source.setUrl(testRepositoryServer.getServerUrl());

    repository.save(source);
  }

}



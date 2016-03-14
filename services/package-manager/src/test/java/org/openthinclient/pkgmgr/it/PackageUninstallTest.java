package org.openthinclient.pkgmgr.it;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.DebianTestRepositoryServer;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = PackageInstallTest.PackageManagerConfig.class)
public class PackageUninstallTest {

    @ClassRule
    public static final DebianTestRepositoryServer testRepositoryServer = new DebianTestRepositoryServer();
    @Autowired
    PackageManagerConfiguration configuration;
    @Autowired
    SourceRepository sourceRepository;
    @Autowired
    PackageRepository packageRepository;
  @Autowired
  PackageManagerFactory packageManagerFactory;

  @Test
  public void testUninstallSinglePackage() throws Exception {
    final PackageManager packageManager = preparePackageManager();

    installPackages(packageManager);
  }

  private void installPackages(PackageManager packageManager) throws Exception {

      fail("missing implementation right now");

//    Collection<org.openthinclient.pkgmgr.db.Package> installables = packageManager.getInstallablePackages();
//
//    assertTrue(!installables.isEmpty());
//	    assertTrue(packageManager.install(installables));
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

    //assertEquals(0, packageManager.getInstallablePackages().size());
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

}



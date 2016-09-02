package org.openthinclient.pkgmgr.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.DebianTestRepositoryServer;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.PackageTestUtils;
import org.openthinclient.pkgmgr.SimpleTargetDirectoryPackageManagerConfiguration;
import org.openthinclient.pkgmgr.TestDirectoryProvider;
import org.openthinclient.pkgmgr.db.InstallationLogEntryRepository;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageInstalledContentRepository;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.exception.SourceIntegrityViolationException;
import org.openthinclient.pkgmgr.op.DefaultPackageOperationContext;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.op.PackageOperationInstall;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.util.dpkg.DefaultLocalPackageRepository;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PackageInstallTest.PackageManagerConfig.class)

public class PackageSourcesHandlingTest {

    @ClassRule
    public static final DebianTestRepositoryServer testRepositoryServer = new DebianTestRepositoryServer();
    PackageManagerConfiguration configuration;
    @Autowired
    ObjectFactory<PackageManagerConfiguration> packageManagerConfigurationObjectFactory;
    @Autowired
    PackageManagerFactory packageManagerFactory;
    @Autowired
    PackageRepository packageRepository;
    @Autowired
    InstallationLogEntryRepository installationLogEntryRepository;
    @Autowired
    PackageInstalledContentRepository packageInstalledContentRepository;

    private PackageManager packageManager;

    @Before
    public void setupTestdir() throws Exception {
        configuration = packageManagerConfigurationObjectFactory.getObject();
        packageManager = preparePackageManager();
    }

    @After
    public void cleanup() throws IOException {
      Files.walkFileTree(configuration.getInstallDir().getParentFile().toPath(), new RecursiveDeleteFileVisitor());
      // Restore Repo to be consistent on each test
      installationLogEntryRepository.deleteAll();
      installationLogEntryRepository.flush();
      
      packageInstalledContentRepository.deleteAll();
      packageInstalledContentRepository.flush();
      
      packageRepository.delete(packageManager.getInstalledPackages());
      packageRepository.flush();      
    }

    @Test(expected=SourceIntegrityViolationException.class)
    @Ignore("JN FIXME")
    public void testAddPackageSource() throws IOException, SourceIntegrityViolationException {
      Source source = createEnabledSource("http://loclahost:9090", "Description");
      packageManager.getSourceRepository().saveAndFlush(source);
      
      List<Source> list = packageManager.getSourceRepository().findAll();
      assertFalse(list.isEmpty());
      

      Package pkg = createPackage("foo", "2.0-1", "foo_2.0-1_i386.deb", list.get(0));
      final Path testdir = TestDirectoryProvider.get();
      Files.createDirectories(testdir);

      final DefaultLocalPackageRepository repo = new DefaultLocalPackageRepository(testdir.resolve("archive"));

      repo.addPackage(pkg, targetPath -> Files.copy(getClass().getResourceAsStream("/test-repository/foo_2.0-1_i386.deb"), targetPath));

      final PackageOperationInstall op = new PackageOperationInstall(pkg);
      final Path installDir = testdir.resolve("install");
      op.execute(new DefaultPackageOperationContext(repo, new PackageManagerDatabase(null, packageRepository, null, null, packageInstalledContentRepository), null, installDir, pkg));
      
      packageManager.deleteSource(list.get(0));
    }

    private Package createPackage(String name, String version, String filename, Source source) {

      final Package pkg = new Package();
      pkg.setSource(source);
      pkg.setName(name);
      pkg.setVersion(version);
      pkg.setFilename(filename);
      return pkg;
    }
   
    
    private Source createEnabledSource(String url, String description) throws MalformedURLException {
      final Source source = new Source();
      source.setDescription(description);
      source.setEnabled(true);
      source.setUrl(new URL(url));
      return source;
    }    
    
    private PackageManager preparePackageManager() throws Exception {
        final PackageManager packageManager = packageManagerFactory.createPackageManager(configuration);

        PackageTestUtils.configureSources(testRepositoryServer, packageManager);

        assertNotNull("sources-list could not be loaded", packageManager.getSourcesList());
        assertEquals("number of entries in sources list is not correct", 1, packageManager.getSourcesList().getSources().size());
        assertEquals("wrong URL of repository", testRepositoryServer.getServerUrl(), packageManager.getSourcesList().getSources().get(0).getUrl());

        //assertEquals(0, packageManager.findByInstalledFalse().size());
        final ListenableProgressFuture<PackageListUpdateReport> updateFuture = packageManager.updateCacheDB();

        assertNotNull("couldn't update cache-DB", updateFuture.get());
        assertEquals("wrong number of installables packages", 19, packageManager.getInstallablePackages().size());
        
        
        return packageManager;
    }

    @Configuration()
    @Import(SimpleTargetDirectoryPackageManagerConfiguration.class)
    public static class PackageManagerConfig {

    }

}



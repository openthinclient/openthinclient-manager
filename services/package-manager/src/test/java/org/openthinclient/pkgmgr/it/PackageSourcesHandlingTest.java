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
import org.openthinclient.pkgmgr.TestDirectoryProvider;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageInstalledContentRepository;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.exception.SourceIntegrityViolationException;
import org.openthinclient.pkgmgr.op.DefaultPackageOperationContext;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.op.PackageOperationInstall;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.progress.NoopProgressReceiver;
import org.openthinclient.util.dpkg.DefaultLocalPackageRepository;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.openthinclient.pkgmgr.PackagesUtil.PACKAGES_SIZE;
import static org.openthinclient.pkgmgr.it.PackageManagerTestUtils.doInstallPackages;
import static org.openthinclient.pkgmgr.it.PackageManagerTestUtils.doUninstallPackages;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PackageInstallTest.PackageManagerConfig.class)
@Sql(executionPhase=ExecutionPhase.AFTER_TEST_METHOD, scripts="classpath:sql/empty-tables.sql")
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
    SourceRepository sourceRepository;    
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
    }

    @Test(expected=SourceIntegrityViolationException.class)
    public void testDeletePackageSourceThrowsException() throws IOException, SourceIntegrityViolationException {
      
      // create a source
      Source source = createEnabledSource(testRepositoryServer.getServerUrl().toString(), "Description: " + testRepositoryServer.hashCode());
      source = packageManager.saveSource(source);
      
      // check if source was saved
      List<Source> list = sourceRepository.findAll();
      assertEquals(2, list.size());
      assertTrue(list.contains(source));
      
      // install a package for source
      installPackage(source);
      
      packageManager.deleteSource(source);
    }
    
    @Test
    public void testDeletePackageSource() throws Exception {
      
      // create a source
      Source _source = createEnabledSource(testRepositoryServer.getServerUrl().toString(), "Description: " + testRepositoryServer.hashCode());
      final Source source = packageManager.saveSource(_source);
      
      // check if source was saved
      List<Source> list = sourceRepository.findAll();
      assertFalse(list.isEmpty());
      assertEquals(2, list.size());
      assertTrue(list.contains(source));
      
      // install a package for source
      Package pkg = createPackage("foo", "2.0-1", "foo_2.0-1_i386.deb", source);
      packageRepository.save(pkg); 
      doInstallPackages(packageManager, Arrays.asList(pkg));
      
      // uninstall package first, then delete source
      doUninstallPackages(packageManager, Arrays.asList(pkg));
      
      ListenableProgressFuture<PackageListUpdateReport> future = packageManager.deleteSourcePackagesFromCacheDB(source);
      future.addCallback((e) -> {
         try {
            packageManager.deleteSource(source);
         } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         } 
      }, 
            (e) -> {});

    }    

    /**
     * Installs a package for given source
     * @param source - the source
     * @throws IOException - if repository installation fails
     */
    private Package installPackage(Source source) throws IOException {
      // create a package with given source
      Package pkg = createPackage("foo", "2.0-1", "foo_2.0-1_i386.deb", source);
      final Path testdir = TestDirectoryProvider.get();
      Files.createDirectories(testdir);
      // create repo-file for package
      final DefaultLocalPackageRepository repo = new DefaultLocalPackageRepository(testdir.resolve("archive"));
      repo.addPackage(pkg, targetPath -> Files.copy(getClass().getResourceAsStream("/test-repository/foo_2.0-1_i386.deb"), targetPath));
      // perform install operation
      final PackageOperationInstall op = new PackageOperationInstall(pkg);
      final Path installDir = testdir.resolve("install");
      op.execute(new DefaultPackageOperationContext(repo, new PackageManagerDatabase(null, packageRepository, null, null, packageInstalledContentRepository), null, installDir, pkg), new NoopProgressReceiver());
      
      return pkg;
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
        assertEquals("wrong number of installables packages", PACKAGES_SIZE, packageManager.getInstallablePackages().size());
        
        return packageManager;
    }

    @Configuration()
    @Import(SimpleTargetDirectoryPackageManagerConfiguration.class)
    public static class PackageManagerConfig {

    }

}



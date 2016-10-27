package org.openthinclient.pkgmgr.it;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.openthinclient.pkgmgr.PackageTestUtils.configureSources;
import static org.openthinclient.pkgmgr.PackageTestUtils.getFilePathsInPackage;
import static org.openthinclient.pkgmgr.PackagesUtil.PACKAGES_SIZE;
import static org.openthinclient.pkgmgr.it.PackageManagerTestUtils.doInstallPackages;
import static org.openthinclient.pkgmgr.it.PackageManagerTestUtils.doUninstallPackages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.openthinclient.pkgmgr.db.Version;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PackageInstallTest.PackageManagerConfig.class)

public class PackageUninstallTest {

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
        Files.walkFileTree(configuration.getInstallDir().getParentFile().toPath(), new RecursiveDeleteFileVisitor());
    }

    @Test
    public void testUninstallSinglePackageFoo() throws Exception {
        final List<Package> packages = packageManager.getInstallablePackages().stream()
                .filter(pkg -> pkg.getName().equals("foo"))
                .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                .collect(Collectors.toList());
        assertContainsPackage(packages, "foo", "2.0-1");

        installPackages(packageManager, packages);

        doUninstallPackages(packageManager, packages);

        assertTestinstallDirectoryEmpty();
        
        // TODO jn: install-dir contains 'schema, sfs, version'-directories, but installed packages has been deleted, should this be fixed?
        // assertInstallDirectoryEmpty();
    }

    @Test
    public void testUninstallSinglePackageFooNeededByBar2() throws Exception {
        final List<Package> packages = packageManager.getInstallablePackages().stream()
                .filter(pkg -> pkg.getName().equals("foo") || pkg.getName().equals("bar2"))
                .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                .collect(Collectors.toList());
        assertContainsPackage(packages, "foo", "2.0-1");
        assertContainsPackage(packages, "bar2", "2.0-1");

        installPackages(packageManager, packages);

        final List<Package> uninstallList = packages.stream().filter(
                pkg -> pkg.getName().equals("foo")).collect(Collectors.toList());
        doUninstallPackages(packageManager, uninstallList);

        assertTestinstallDirectoryEmpty();
        
        // TODO jn: install-dir contains 'schema, sfs, version'-directories, but installed packages has been deleted, should this be fixed?
        // assertInstallDirectoryEmpty();
    }

    private void assertFileExists(Path path) {
        assertTrue(path.getFileName() + " does not exist", Files.exists(path));
        assertTrue(path.getFileName() + " is not a regular file", Files.isRegularFile(path));
    }

    private void assertTestinstallDirectoryEmpty() throws IOException {
        final Path testInstallDirectory = configuration.getTestinstallDir().toPath();
        assertEquals("test-install-directory isn't empty", 0, Files.list(testInstallDirectory).count());
    }

    private void assertInstallDirectoryEmpty() throws IOException {
        final Path installDirectory = configuration.getInstallDir().toPath();
        if (Files.list(installDirectory).count() > 0) {
            final Stream<Path> contents = Files.list(installDirectory);

            final String contentsString = contents
                    .map(path -> installDirectory.relativize(path).toString())
                    .collect(Collectors.joining(", "));

            fail("install-directory isn't empty: " + contentsString);
        }
    }

    private void assertContainsPackage(final List<Package> packages, final String packageName, final String version) {
        assertTrue("missing " + packageName + " package (Version: " + version + " )",
                packages.stream()
                        .filter(p -> p.getName().equals(packageName))
                        .filter(p -> p.getVersion().equals(Version.parse(version)))
                        .findFirst().isPresent());
    }

    private void installPackages(PackageManager packageManager, List<Package> packages) throws Exception {

        doInstallPackages(packageManager, packages);
        final Path installDirectory = configuration.getInstallDir().toPath();

        for (Package pkg : packages) {
            String pkgName = pkg.getName();
            Path[] pkgPath = getFilePathsInPackage(pkgName, installDirectory);
            for (Path file : pkgPath)
                assertFileExists(file);
        }
        assertTestinstallDirectoryEmpty();
    }

    private PackageManager preparePackageManager() throws Exception {


        PackageManager packageManager = packageManagerFactory.createPackageManager(configuration);
        configureSources(testRepositoryServer, packageManager);

        assertNotNull(packageManager.getSourcesList());
        assertEquals(1, packageManager.getSourcesList().getSources().size());
        assertEquals(testRepositoryServer.getServerUrl(), packageManager.getSourcesList().getSources().get(0).getUrl());

        packageManager.updateCacheDB().get();
        assertEquals(PACKAGES_SIZE, packageManager.getInstallablePackages().size());
        return packageManager;
    }

    @Configuration()
    @Import(SimpleTargetDirectoryPackageManagerConfiguration.class)
    public static class PackageManagerConfig {

    }

}



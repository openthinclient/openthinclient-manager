package org.openthinclient.pkgmgr.it;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.*;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.progress.ListenableProgressFuture;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.openthinclient.pkgmgr.PackageTestUtils.getFilePathsInPackage;
import static org.openthinclient.pkgmgr.PackagesUtil.PACKAGES_SIZE;
import static org.openthinclient.pkgmgr.it.PackageManagerTestUtils.doInstallPackages;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PackageInstallTest.PackageManagerConfig.class)
@Sql(executionPhase=ExecutionPhase.BEFORE_TEST_METHOD, scripts="classpath:sql/empty-tables.sql")
@Sql(executionPhase=ExecutionPhase.AFTER_TEST_METHOD,  scripts="classpath:sql/empty-tables.sql")
public class PackageUpdateTest {

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
    public void cleanup() throws IOException {
      Files.walkFileTree(configuration.getInstallDir().getParentFile().toPath(), new RecursiveDeleteFileVisitor());
    }

    @Test
    public void testUpdateSinglePackageFoo() throws Exception {
        final List<Package> packages = packageManager.getInstallablePackages().stream()
                .filter(pkg -> pkg.getName().equals("foo"))
                .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                .collect(Collectors.toList());
        assertContainsPackage(packages, "foo", "2.0-1");

        installPackages(packageManager, packages);

        final Package packageToUpdate = packageManager.getInstallablePackages().stream()
                .filter(pkg -> pkg.getName().equals("foo") && pkg.getVersion().equals(Version.parse("2.1-1")))
                .findFirst().get();

        assertVersion("foo", "2.0-1");
        doInstallPackages(packageManager, Collections.singletonList(packageToUpdate));

        final Path installDirectory = configuration.getInstallDir().toPath();
        Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
        for (Path file : pkgPath)
            assertFileExists(file);

        assertVersion("foo", "2.1-1");
        assertTestinstallDirectoryEmpty();
    }

    @Test
    public void testChangePackageStatusBySource() throws Exception {
       
       final List<Package> packages = packageManager.getInstallablePackages().stream()
             .filter(pkg -> pkg.getName().equals("foo") || pkg.getName().equals("bas"))
             .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
             .collect(Collectors.toList());
       
       assertContainsPackage(packages, "foo", "2.0-1");
       assertContainsPackage(packages, "bas", "2.0-1");
       
       Package somePackage = getPackageFromList(packages, "foo", "2.0-1").get();
       
       Source source = somePackage.getSource();
       source.setEnabled(false);
       packageManager.saveSource(source);
       
       assertTrue(packageManager.getInstallablePackages().isEmpty());
       
       // restore disabled source
       source.setEnabled(true);
       packageManager.saveSource(source);
    }

    /**
     * @see https://support.openthinclient.com/openthinclient/browse/SOFTWARE-505
     * @see Scenario: Update eines Pakets, dabei sollen andere Pakete, die die Abhängigkeiten erfüllen, unberührt bleiben - SOFTWARE-505
     * @throws Exception
     */
    @Test
    public void testUpdatePackageDependingOnNewerVersion() throws Exception {
        final List<Package> packages = packageManager.getInstallablePackages().stream()
                .filter(pkg -> pkg.getName().equals("bas") || pkg.getName().equals("bar2") || pkg.getName().equals("bar2-dev") )
                .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                .collect(Collectors.toList());

        Package foo = packageManager.getInstallablePackages().stream()
                .filter(pkg -> pkg.getName().equals("foo") && pkg.getVersion().equals(Version.parse("2.1-1")))
                .findFirst().get();
        packages.add(foo);

        assertContainsPackage(packages, "foo", "2.1-1");
        assertContainsPackage(packages, "bas", "2.0-1");
        assertContainsPackage(packages, "bar2", "2.0-1");
        assertContainsPackage(packages, "bar2-dev", "2.0-1");

        installPackages(packageManager, packages);
        assertVersion("foo", "2.1-1");
        assertVersion("bas", "2.0-1");
        assertVersion("bar2", "2.0-1");
        assertVersion("bar2-dev", "2.1-1"); // has wrong version-number (should be 2.0-1) in version.txt

        final Package packageToUpdate = packageManager.getInstallablePackages().stream()
                .filter(pkg -> pkg.getName().equals("bas") && pkg.getVersion().equals(Version.parse("2.1-1")))
                .findFirst().get();

        doInstallPackages(packageManager, Collections.singletonList(packageToUpdate));

        final Path installDirectory = configuration.getInstallDir().toPath();
        Path[] pkgPath = getFilePathsInPackage("foo", installDirectory);
        for (Path file : pkgPath)
            assertFileExists(file);
        pkgPath = getFilePathsInPackage("bas", installDirectory);
        for (Path file : pkgPath)
            assertFileExists(file);

        assertVersion("foo", "2.1-1");
        assertVersion("bas", "2.1-1");
        assertVersion("bar2", "2.0-1");
        assertVersion("bar2-dev", "2.1-1"); // has wrong version-number (should be 2.0-1) in version.txt
        assertTestinstallDirectoryEmpty();
    }

    @Test
    public void testUpdatePackageDependingOnNotInstalledPackage() throws Exception {
        final List<Package> packages = packageManager.getInstallablePackages().stream()
                .filter(pkg -> pkg.getName().equals("zonk"))
                .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                .collect(Collectors.toList());
        assertContainsPackage(packages, "zonk", "2.0-1");

        installPackages(packageManager, packages);

        assertVersion("zonk", "2.0-1");
        final Package packageToUpdate = packageManager.getInstallablePackages().stream()
                .filter(pkg -> pkg.getName().equals("zonk") && pkg.getVersion().equals(Version.parse("2.1-1")))
                .findFirst().get();

        doInstallPackages(packageManager, Collections.singletonList(packageToUpdate));

        final Path installDirectory = configuration.getInstallDir().toPath();
        Path[] pkgPath = getFilePathsInPackage("zonk", installDirectory);
        for (Path file : pkgPath)
            assertFileExists(file);
        pkgPath = getFilePathsInPackage("foo", installDirectory);
        for (Path file : pkgPath)
            assertFileExists(file);

        assertVersion("zonk", "2.1-1");
        assertTestinstallDirectoryEmpty();
    }

    @Test
    public void testUpdatePackageReplacingOtherPackage() throws Exception {
        final List<Package> packages = packageManager.getInstallablePackages().stream()
                .filter(pkg -> pkg.getName().equals("foo") || pkg.getName().equals("bar2"))
                .filter(pkg -> pkg.getVersion().equals(Version.parse("2.0-1")))
                .collect(Collectors.toList());
        assertContainsPackage(packages, "foo", "2.0-1");
        assertContainsPackage(packages, "bar2", "2.0-1");

        installPackages(packageManager, packages);

        assertVersion("bar2", "2.0-1");
        final Package packageToUpdate = packageManager.getInstallablePackages().stream()
                .filter(pkg -> pkg.getName().equals("bar2") && pkg.getVersion().equals(Version.parse("2.1-1")))
                .findFirst().get();

        doInstallPackages(packageManager, Collections.singletonList(packageToUpdate));

        final Path installDirectory = configuration.getInstallDir().toPath();
        Path[] pkgPath = getFilePathsInPackage("bar2", installDirectory);
        for (Path file : pkgPath)
            assertFileExists(file);
        pkgPath = getFilePathsInPackage("foo", installDirectory);
        // package foo still exists, but should be replaced by 'bar2'
        for (Path file : pkgPath)
            assertFileNotExists(file);

        assertVersion("bar2", "2.1-1");
        assertTestinstallDirectoryEmpty();
    }

    private void assertFileExists(Path path) {
        assertTrue(path.getFileName() + " does not exist", Files.exists(path));
        assertTrue(path.getFileName() + " is not a regular file", Files.isRegularFile(path));
    }

    private void assertFileNotExists(Path path) {
        assertTrue(path.getFileName() + " does exist", !Files.exists(path));
    }

    private void assertTestinstallDirectoryEmpty() throws IOException {
        final Path testInstallDirectory = configuration.getTestinstallDir().toPath();
        assertEquals("test-install-directory isn't empty", 0, Files.list(testInstallDirectory).count());
    }

    private void assertContainsPackage(final List<Package> packages, final String packageName, final String version) {
        assertTrue("missing " + packageName + " package (Version: " + version + " )",
                packages.stream()
                        .filter(p -> p.getName().equals(packageName))
                        .filter(p -> p.getVersion().equals(Version.parse(version)))
                        .findFirst().isPresent());
    }

    private Optional<Package> getPackageFromList(final List<Package> packages, final String packageName, final String version) {
         return packages.stream()
             .filter(p -> p.getName().equals(packageName))
             .filter(p -> p.getVersion().equals(Version.parse(version)))
             .findFirst();
    }

    private void assertVersion(final String pkg, final String version) throws Exception {
        final Path installDirectory = configuration.getInstallDir().toPath();
        final Path versionFile = installDirectory.resolve("version").resolve(pkg + "-version.txt");
        assertFileExists(versionFile);
        try (final BufferedReader in = new BufferedReader(new FileReader(versionFile.toFile()))) {
            assertEquals("wrong package version", version, in.readLine().trim());
            in.close();
        }
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
        final PackageManager packageManager = packageManagerFactory.createPackageManager(configuration);

        PackageTestUtils.configureSources(testRepositoryServer, packageManager);

//        assertNotNull(packageManager.getSourcesList());
//        assertEquals("Sources list size does not fit", 1, packageManager.getSourcesList().getSources().size());
//        assertEquals(testRepositoryServer.getServerUrl(), packageManager.getSourcesList().getSources().get(0).getUrl());
//
//        assertEquals("Expect 0 installable packages", 0, packageManager.getInstallablePackages().size());
//        packageManager.updateCacheDB().get();
//        assertEquals("Expected size of 'installable packages' does not fit", 19, packageManager.getInstallablePackages().size());

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



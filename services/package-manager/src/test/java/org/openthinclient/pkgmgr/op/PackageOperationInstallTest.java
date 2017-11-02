package org.openthinclient.pkgmgr.op;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openthinclient.pkgmgr.TestDirectoryProvider;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageInstalledContent;
import org.openthinclient.pkgmgr.db.PackageInstalledContentRepository;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.progress.NoopProgressReceiver;
import org.openthinclient.util.dpkg.DefaultLocalPackageRepository;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PackageOperationInstallTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    PackageInstalledContentRepository installedContentRepository;
    @Mock
    PackageRepository packageRepository;

    @Test
    public void testInstallPackage() throws Exception {

        Package pkg = createPackage("foo", "2.0-1", "foo_2.0-1_i386.deb");
        final Path testdir = TestDirectoryProvider.get();
        Files.createDirectories(testdir);

        final DefaultLocalPackageRepository repo = new DefaultLocalPackageRepository(testdir.resolve("archive"));

        repo.addPackage(pkg, targetPath -> Files.copy(getClass().getResourceAsStream("/test-repository/foo_2.0-1_i386.deb"), targetPath));

        final PackageOperationInstall op = new PackageOperationInstall(pkg);

        final Path installDir = testdir.resolve("install");
        op.execute(new DefaultPackageOperationContext(repo, new PackageManagerDatabase(null, packageRepository, null, null, installedContentRepository), null, installDir, pkg), new NoopProgressReceiver());

        assertDirectory(installDir, "schema");
        assertDirectory(installDir, "schema/application");
        assertFile(installDir, "schema/application/foo-tiny.xml.sample", "e5b0268ae229188d1b434fe34879aa645f1d09ab");
        assertFile(installDir, "schema/application/foo.xml", "58091c6fbbf30c7e10b971865ab4413973583bb3");
        assertDirectory(installDir, "sfs");
        assertDirectory(installDir, "sfs/package");
        assertFile(installDir, "sfs/package/foo.sfs", "49c266b761aa645ad47e22a75da60e992654b427");
        assertDirectory(installDir, "version");
        assertFile(installDir, "version/version.txt~", "da39a3ee5e6b4b0d3255bfef95601890afd80709");
        assertFile(installDir, "version/foo-version.txt", "5859d094ddc1848ef3d7fc6ff1d02d75d6ec1d95");

        final List<PackageInstalledContent> expected = Arrays.asList(
                installedDirectory(pkg, 0, "schema"), //
                installedDirectory(pkg, 1, "schema/application"), //
                installedFile(pkg, 2, "schema/application/foo-tiny.xml.sample", "e5b0268ae229188d1b434fe34879aa645f1d09ab"), //
                installedFile(pkg, 3, "schema/application/foo.xml", "58091c6fbbf30c7e10b971865ab4413973583bb3"), //
                installedDirectory(pkg, 4, "sfs"), //
                installedDirectory(pkg, 5, "sfs/package"), //
                installedFile(pkg, 6, "sfs/package/foo.sfs", "49c266b761aa645ad47e22a75da60e992654b427"), //
                installedDirectory(pkg, 7, "version"), //
                installedFile(pkg, 8, "version/version.txt~", "da39a3ee5e6b4b0d3255bfef95601890afd80709"), //
                installedFile(pkg, 9, "version/foo-version.txt", "5859d094ddc1848ef3d7fc6ff1d02d75d6ec1d95") //
        );

        Mockito.verify(installedContentRepository).save(expected);
    }

    private PackageInstalledContent installedDirectory(Package pkg, int sequence, String path) {
        final PackageInstalledContent content = new PackageInstalledContent();
        content.setType(PackageInstalledContent.Type.DIR);
        content.setSequence(sequence);
        content.setPackage(pkg);
        content.setPath(Paths.get(path));
        return content;
    }

    private PackageInstalledContent installedFile(Package pkg, int sequence, String path, String sha1) {
        final PackageInstalledContent content = new PackageInstalledContent();
        content.setType(PackageInstalledContent.Type.FILE);
        content.setSequence(sequence);
        content.setPackage(pkg);
        content.setPath(Paths.get(path));
        content.setSha1(sha1);
        return content;
    }

    private void assertFile(Path baseDirectory, String file, String sha1) throws Exception {
        assertTrue("Expected " + file + " to be a file", Files.isRegularFile(baseDirectory.resolve(file)));

        assertEquals("Incorrect SHA1 checksum for " + file, sha1.toLowerCase(), computeSHA1(baseDirectory.resolve(file)));

    }

    private String computeSHA1(Path file) throws Exception {

        final MessageDigest digest = MessageDigest.getInstance("SHA1");

        byte[] buf = new byte[10 * 1024 * 1024];
        try (final InputStream in = Files.newInputStream(file)) {
            int read;

            while ((read = in.read(buf)) > -1) {
                digest.update(buf, 0, read);
            }

            return PackageOperationDownload.byteArrayToHexString(digest.digest()).toLowerCase();
        }

    }

    private void assertDirectory(Path baseDirectory, String directory) {
        assertTrue("Expected " + directory + " to be a directory", Files.isDirectory(baseDirectory.resolve(directory)));
    }

    private Package createPackage(String name, String version, String filename) {

        final Package pkg = new Package();
        final Source source = new Source();
        source.setId(1L);
        pkg.setSource(source);
        pkg.setName(name);
        pkg.setVersion(version);
        pkg.setFilename(filename);
        return pkg;
    }
}
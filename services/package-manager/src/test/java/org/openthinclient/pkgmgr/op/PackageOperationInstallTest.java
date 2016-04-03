package org.openthinclient.pkgmgr.op;

import org.junit.Test;
import org.openthinclient.pkgmgr.TestDirectoryProvider;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.util.dpkg.DefaultLocalPackageRepository;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PackageOperationInstallTest {

    @Test
    public void testInstallPackage() throws Exception {

        Package pkg = createPackage("foo", "2.0-1", "foo_2.0-1_i386.deb");
        final Path testdir = TestDirectoryProvider.get();
        Files.createDirectories(testdir);

        final DefaultLocalPackageRepository repo = new DefaultLocalPackageRepository(testdir.resolve("archive"));

        repo.addPackage(pkg, targetPath -> Files.copy(getClass().getResourceAsStream("/test-repository/foo_2.0-1_i386.deb"), targetPath));

        final PackageOperationInstall op = new PackageOperationInstall(pkg);

        final Path installDir = testdir.resolve("install");
        op.execute(new DefaultPackageOperationContext(repo, null, installDir, pkg));

        assertDirectory(installDir, "schema");
        assertDirectory(installDir, "schema/application");
        assertFile(installDir, "schema/application/foo-tiny.xml.sample", "727ee770340912d0210e7de2b730aeac");
        assertFile(installDir, "schema/application/foo.xml", "593974c086a3d94088c9e6c7de4fa203");
        assertDirectory(installDir, "sfs");
        assertDirectory(installDir, "sfs/package");
        assertFile(installDir, "sfs/package/foo.sfs", "12fd6fac463ddec307a7516a2a3a0a35");
        assertDirectory(installDir, "version");
        assertFile(installDir, "version/version.txt~", "d41d8cd98f00b204e9800998ecf8427e");
        assertFile(installDir, "version/foo-version.txt", "2f7296af2571bff2e496eb88aecb7aa4");


    }

    private void assertFile(Path baseDirectory, String file, String md5) throws Exception {
        assertTrue("Expected " + file + " to be a file", Files.isRegularFile(baseDirectory.resolve(file)));

        assertEquals(md5.toLowerCase(), computeMD5(baseDirectory.resolve(file)));

    }

    private String computeMD5(Path file) throws Exception {

        final MessageDigest digest = MessageDigest.getInstance("MD5");

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
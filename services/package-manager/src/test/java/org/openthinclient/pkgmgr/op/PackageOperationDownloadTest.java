package org.openthinclient.pkgmgr.op;

import org.junit.ClassRule;
import org.junit.Test;
import org.openthinclient.manager.util.http.NotFoundException;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.manager.util.http.impl.HttpClientDownloadManager;
import org.openthinclient.pkgmgr.DebianTestRepositoryServer;
import org.openthinclient.pkgmgr.PackageChecksumVerificationFailedException;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.progress.NoopProgressReceiver;
import org.openthinclient.util.dpkg.DefaultLocalPackageRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class PackageOperationDownloadTest {

    @ClassRule
    public static DebianTestRepositoryServer TEST_REPO_SERVER = new DebianTestRepositoryServer();

    @Test
    public void testDownloadFile() throws Exception {

        final Package pkg = createPackage("foo", "2.0-1", "./foo_2.0-1_i386.deb", "f20b39b2e66616a6a3aaf0156305501c");

        final Path repoTarget = createTestDirectory("testDownloadFile");

        final PackageOperationDownload dl = new PackageOperationDownload(pkg, createDownloadManager());
        dl.execute(new DefaultPackageOperationContext(new DefaultLocalPackageRepository(repoTarget), null, null, null, pkg), new NoopProgressReceiver());

        assertTrue(Files.exists(repoTarget.resolve("736").resolve("foo_2.0-1_i386.deb")));
    }

    private HttpClientDownloadManager createDownloadManager() {
        return new HttpClientDownloadManager(new NetworkConfiguration.ProxyConfiguration(), PackageOperationDownloadTest.class.getName());
    }

    private Path createTestDirectory(String testName) {
        return Paths.get("target", testName +
                "-" + System.currentTimeMillis());
    }

    private Package createPackage(String name, String version, String filename, String md5sum) {
        final Source source = new Source();
        source.setEnabled(true);
        source.setUrl(TEST_REPO_SERVER.getServerUrl());
        source.setId(736L);

        final Package pkg = new Package();
        pkg.setSource(source);
        pkg.setName(name);
        pkg.setVersion(Version.parse(version));
        pkg.setFilename(filename);
        pkg.setMD5sum(md5sum);
        return pkg;
    }

    // FIXME find a better exception type. Something like NotFoundException
    @Test(expected = NotFoundException.class)
    public void testNonExistingPackage() throws Exception {

        final Package pkg = createPackage("fooXX", "2.0-1", "./fooXX_2.0-1_i386.deb", "f20b39b2e66616a6a3aaf0156305501c");

        final Path repoTarget = createTestDirectory("testNonExistingPackage");

        final PackageOperationDownload dl = new PackageOperationDownload(pkg, createDownloadManager());
        dl.execute(new DefaultPackageOperationContext(new DefaultLocalPackageRepository(repoTarget), null, null, null, pkg), new NoopProgressReceiver());
    }

    @Test(expected = PackageChecksumVerificationFailedException.class)
    public void testMismatchingChecksum() throws Exception {

        final Package pkg = createPackage("foo", "2.0-1", "./foo_2.0-1_i386.deb", "ababababababababababababababababab");

        final Path repoTarget = createTestDirectory("testMismatchingChecksum");

        final PackageOperationDownload dl = new PackageOperationDownload(pkg, createDownloadManager());
        dl.execute(new DefaultPackageOperationContext(new DefaultLocalPackageRepository(repoTarget), null, null, null, pkg), new NoopProgressReceiver());

    }
}
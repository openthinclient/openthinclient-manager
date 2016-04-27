package org.openthinclient.pkgmgr;

import org.junit.Assert;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.db.Version;

import java.util.List;

public class PackageTestUtils {
    public static Package createPackage(String name, String version) {
        final Package pkg = new Package();
        pkg.setName(name);
        pkg.setVersion(version);
        return pkg;
    }

    public static Version createVersion(String upstreamVersions, String debianRevision) {
        final Version version = new Version();
        version.setUpstreamVersion(upstreamVersions);
        version.setDebianRevision(debianRevision);
        return version;
    }

    public static void configureSources(DebianTestRepositoryServer server, PackageManager packageManager) {
        final SourceRepository sourceRepository = packageManager.getSourceRepository();

        final List<Source> existing = sourceRepository.findAll();

        if (existing.size() == 0) {
            final Source source = new Source();
            source.setEnabled(true);
            source.setUrl(server.getServerUrl());
            sourceRepository.saveAndFlush(source);

        } else if (existing.size() == 1) {
            final Source existingSource = existing.get(0);
            Assert.assertEquals(server.getServerUrl(), existingSource.getUrl());
        } else {
            Assert.fail("More than a single source has been registered");
        }


    }
}

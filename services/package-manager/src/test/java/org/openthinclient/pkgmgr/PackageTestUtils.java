package org.openthinclient.pkgmgr;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.Version;
import org.springframework.util.CollectionUtils;

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
//        final SourceRepository sourceRepository = packageManager.getSourceRepository();
//        final List<Source> existing = sourceRepository.findAll();

        final List<Source> existing = new ArrayList<>(packageManager.findAllSources());
      
        if (existing.size() == 0) {
            final Source source = new Source();
            source.setEnabled(true);
            source.setUrl(server.getServerUrl());
            packageManager.saveSource(source);

        } else if (existing.size() == 1) {
            final Source existingSource = existing.get(0);
            Assert.assertEquals(server.getServerUrl(), existingSource.getUrl());
        } else {
            Assert.fail("More than a single source has been registered");
        }


    }

    public static Path[] getFilePathsInPackage(String pkg, Path directory) {
        Path[] filePaths = new Path[3];
        filePaths[0] = directory.resolve("schema").resolve("application").resolve(pkg + ".xml");
        filePaths[1] =
                directory.resolve("schema").resolve("application").resolve(pkg + "-tiny.xml.sample");
        filePaths[2] = directory.resolve("sfs").resolve("package").resolve(pkg + ".sfs");
        return filePaths;
    }
}

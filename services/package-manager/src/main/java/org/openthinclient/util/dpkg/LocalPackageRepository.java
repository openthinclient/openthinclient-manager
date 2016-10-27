package org.openthinclient.util.dpkg;

import org.openthinclient.pkgmgr.db.Package;

import java.io.IOException;
import java.nio.file.Path;

public interface LocalPackageRepository {

    Path getPackage(Package pkg);

    boolean isAvailable(Package pkg);

    void addPackage(Package pkg, PackageContentsProvider targetPathConsumer) throws IOException;

    interface PackageContentsProvider {
        void provide(Path packageTargetPath) throws IOException;
    }

}

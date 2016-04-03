package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.db.PackageInstalledContentRepository;
import org.openthinclient.util.dpkg.LocalPackageRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public interface PackageOperationContext {

    LocalPackageRepository getLocalPackageRepository();

    PackageInstalledContentRepository getPackageInstalledContentRepository();

    OutputStream createFile(Path path) throws IOException;

    void createDirectory(Path path) throws IOException;

    void createSymlink(Path link, Path target) throws IOException;

}

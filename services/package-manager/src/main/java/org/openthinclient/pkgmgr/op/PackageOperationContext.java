package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.util.dpkg.LocalPackageRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface PackageOperationContext {

    LocalPackageRepository getLocalPackageRepository();

    PackageManagerDatabase getDatabase();

    OutputStream createFile(Path path) throws IOException;

    void createDirectory(Path path) throws IOException;

    void createSymlink(Path link, Path target) throws IOException;

    InputStream newInputStream(Path path) throws IOException;

    void delete(Path path) throws IOException;

    Stream<Path> list(Path path) throws IOException;

    boolean isRegularFile(Path path) throws IOException;
}

package org.openthinclient.util.dpkg;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultLocalPackageRepository implements LocalPackageRepository {

    private final Path rootPath;

    public DefaultLocalPackageRepository(Path rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public Path getPackage(Package pkg) {

        final Path packagePath = getPackagePath(pkg);

        if (isValidPackageFile(packagePath)) {
            return packagePath;
        }
        return null;
    }

    private boolean isValidPackageFile(Path packagePath) {
        return Files.exists(packagePath) && Files.isRegularFile(packagePath);
    }

    private Path getPackagePath(Package pkg) {

        final int lastSlash = pkg.getFilename().lastIndexOf('/');
        final String filename;
        if (lastSlash > 0) {
            filename = pkg.getFilename().substring(lastSlash + 1);
        } else {
            filename = pkg.getFilename();
        }

        return getSourcePath(pkg.getSource()).resolve(filename);
    }

    private Path getSourcePath(Source source) {
        final Long id = source.getId();

        if (id == null)
            throw new IllegalArgumentException("The given source has not yet been saved");

        return rootPath.resolve("" + id);
    }

    @Override
    public boolean isAvailable(Package pkg) {
        return isValidPackageFile(getPackagePath(pkg));
    }

    @Override
    public void addPackage(Package pkg, PackageContentsProvider packageContentsProvider) throws IOException {

        final Path targetPath = getPackagePath(pkg);

        final Path temporaryPath = targetPath.getParent().resolve(targetPath.getFileName() + ".tmp");

        packageContentsProvider.provide(temporaryPath);

        // the provider completed. Now move the newly generated file to its final position
        Files.move(temporaryPath, targetPath);
    }
}

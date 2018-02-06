package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.db.Installation;
import org.openthinclient.pkgmgr.db.InstallationLogEntry;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.util.dpkg.LocalPackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DefaultPackageOperationContext implements PackageOperationContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPackageOperationContext.class);

    private final PackageManagerDatabase packageManagerDatabase;
    private final Installation installation;
    private final Path targetDirectory;
    private final List<InstallationLogEntry> log;
    private final Package pkg;
    private final LocalPackageRepository localPackageRepository;

    public DefaultPackageOperationContext(LocalPackageRepository localPackageRepository, PackageManagerDatabase packageManagerDatabase, final Installation installation, final Path targetDirectory, Package pkg) {
        this.packageManagerDatabase = packageManagerDatabase;
        this.installation = installation;
        this.localPackageRepository = localPackageRepository;
        this.targetDirectory = targetDirectory;
        this.pkg = pkg;
        log = new ArrayList<>();
    }


    @Override
    public LocalPackageRepository getLocalPackageRepository() {
        return localPackageRepository;
    }

    @Override
    public OutputStream createFile(final Path path) throws IOException {
        LOGGER.info("Creating file {}", path);
        log.add(InstallationLogEntry.file(installation, pkg, path));
        return Files.newOutputStream(combine(targetDirectory, path));
    }

    @Override
    public void createDirectory(final Path path) throws IOException {
        LOGGER.info("Creating directory {}", path);
        log.add(InstallationLogEntry.dir(installation, pkg, path));
        // FIXME shall we really use createDirectorie_s_ here? This could accidently create unwanted ones
        Files.createDirectories(combine(targetDirectory, path));
    }

    @Override
    public void createSymlink(final Path link, final Path target) throws IOException {

        // FIXME if the target is not a relative path, this kind of linking might fail!
        LOGGER.info("Symlinking {} -> {}", link, target);
        log.add(InstallationLogEntry.symlink(installation, pkg, link));
        Files.createLink(combine(targetDirectory, link), target);

    }

    @Override
    public InputStream newInputStream(Path path) throws IOException {
        LOGGER.info("reading file {}", path);
        return Files.newInputStream(combine(targetDirectory, path));
    }

    @Override
    public void delete(Path path) throws IOException {
        final Path actualPath = combine(targetDirectory, path);
        Files.delete(actualPath);
    }

    @Override
    public Stream<Path> list(Path path) throws IOException {
        return Files.list(combine(targetDirectory, path));
    }

    @Override
    public boolean isRegularFile(Path path) {
        return Files.isRegularFile(combine(targetDirectory, path));
    }

    protected Path combine(Path root, Path relative) {
        // FIXME the relative path should be converted to the Filesystem
        return root.resolve(relative);
    }

    public List<InstallationLogEntry> getLog() {
        return log;
    }

    @Override
    public PackageManagerDatabase getDatabase() {
        return packageManagerDatabase;
    }
}

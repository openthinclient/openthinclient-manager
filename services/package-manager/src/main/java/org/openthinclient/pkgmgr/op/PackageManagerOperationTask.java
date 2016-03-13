package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.db.Installation;
import org.openthinclient.pkgmgr.db.InstallationLogEntryRepository;
import org.openthinclient.pkgmgr.db.InstallationRepository;
import org.openthinclient.pkgmgr.db.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.Collection;

public class PackageManagerOperationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageManagerOperationTask.class);

    private final Collection<org.openthinclient.pkgmgr.db.Package> packages;
    private final InstallationRepository installationRepository;
    private final InstallationLogEntryRepository installationLogEntryRepository;

    public PackageManagerOperationTask(final Collection<Package> packages, InstallationRepository installationRepository,
                                       InstallationLogEntryRepository installationLogEntryRepository) {
        this.packages = packages;
        this.installationRepository = installationRepository;
        this.installationLogEntryRepository = installationLogEntryRepository;
    }

    public void execute(PackageManagerConfiguration configuration) throws PackageManagerException, IOException {
        LOGGER.info("Package installation started.");

        final Installation installation = new Installation();
        installation.setStart(LocalDateTime.now());

        // persist the installation first to allow on the go persistence of the installationlogentry entities
        installationRepository.save(installation);

        // FIXME we should verify that the test install directory is actually empty at the moment.
        final Path testInstallDir = configuration.getTestinstallDir().toPath();

        LOGGER.info("Phase 1: Installation into test-install directory ({})", testInstallDir);
        doInstall(configuration, installation, testInstallDir);

        LOGGER.info("Phase 2: Moving installed contents to the destination directory", configuration.getInstallDir());
        doMoveInstalledContents(configuration);

        installation.setEnd(LocalDateTime.now());

        LOGGER.info("Package installation completed.");
    }

    private void doMoveInstalledContents(final PackageManagerConfiguration configuration) throws IOException {

        final Path testInstallDir = configuration.getTestinstallDir().toPath().toAbsolutePath();
        final Path targetDir = configuration.getInstallDir().toPath().toAbsolutePath();

        Files.walkFileTree(testInstallDir, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {

                // create the directory in our installation
                final Path relative = testInstallDir.relativize(dir);
                final Path target = targetDir.resolve(relative);

                Files.createDirectories(target);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {

                final Path relative = testInstallDir.relativize(file);
                final Path target = targetDir.resolve(relative);

                Files.move(file, target);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
                LOGGER.error("Failed to visit installed file {}", file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                // FIXME what should be done in case of exc != null
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });

    }

    private void doInstall(final PackageManagerConfiguration configuration, Installation installation, Path targetDirectory) throws IOException {
        for (Package pkg : packages) {
            final DefaultPackageOperationContext context = new DefaultPackageOperationContext(installation, targetDirectory,
                    pkg);

            final Path localPackageFile = configuration.getArchivesDir().toPath().toAbsolutePath().resolve(pkg.getFilename());

            LOGGER.info("Installing {} ({})", pkg.getName(), localPackageFile);

            final PackageOperationInstall installOp = new PackageOperationInstall(pkg, localPackageFile);
            installOp.execute(context);

            // save the generated log entries

            installationLogEntryRepository.save(context.getLog());

            LOGGER.info("Installation completed.");
        }
    }
}

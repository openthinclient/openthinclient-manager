package org.openthinclient.pkgmgr.op;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.db.Installation;
import org.openthinclient.pkgmgr.db.InstallationLogEntryRepository;
import org.openthinclient.pkgmgr.db.InstallationRepository;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.progress.ProgressReceiver;
import org.openthinclient.pkgmgr.progress.ProgressTask;
import org.openthinclient.util.dpkg.LocalPackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PackageManagerOperationTask implements ProgressTask<PackageManagerOperationReport> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageManagerOperationTask.class);

    private final PackageManagerConfiguration configuration;
    private final DefaultPackageManagerOperation operation;
    private final InstallationRepository installationRepository;
    private final InstallationLogEntryRepository installationLogEntryRepository;
    private final LocalPackageRepository localPackageRepository;
    private final DownloadManager downloadManager;

    public PackageManagerOperationTask(final PackageManagerConfiguration configuration, DefaultPackageManagerOperation operation, InstallationRepository installationRepository,
                                       InstallationLogEntryRepository installationLogEntryRepository, LocalPackageRepository localPackageRepository, DownloadManager downloadManager) {
        this.configuration = configuration;
        this.operation = operation;
        this.installationRepository = installationRepository;
        this.installationLogEntryRepository = installationLogEntryRepository;
        this.localPackageRepository = localPackageRepository;
        this.downloadManager = downloadManager;
    }

    @Override
    public PackageManagerOperationReport execute(ProgressReceiver progressReceiver) throws Exception {
        LOGGER.info("Package installation started.");

        final Installation installation = new Installation();
        installation.setStart(LocalDateTime.now());

        // persist the installation first to allow on the go persistence of the installationlogentry entities
        installationRepository.save(installation);

        LOGGER.info("Determining packages to be downloaded");

        // FIXME we should verify that the test install directory is actually empty at the moment.
        final Path testInstallDir = configuration.getTestinstallDir().toPath();

        downloadPackages(installation, testInstallDir);



        LOGGER.info("Phase 1: Installation into test-install directory ({})", testInstallDir);
        doInstall(configuration, installation, testInstallDir);

        LOGGER.info("Phase 2: Moving installed contents to the destination directory", configuration.getInstallDir());
        doMoveInstalledContents(configuration);

        installation.setEnd(LocalDateTime.now());

        installationRepository.save(installation);

        LOGGER.info("Package installation completed.");

        return new PackageManagerOperationReport();
    }

    private void downloadPackages(Installation installation, Path targetDirectory) throws IOException {
        final List<PackageOperationDownload> downloadOperations = operation.getResolveState() //
                .getInstalling().stream() //
                // filtering out all packages that are already locally available
                .filter(pkg -> !localPackageRepository.isAvailable(pkg)) //
                .map(pkg -> new PackageOperationDownload(pkg, downloadManager)) //
                .collect(Collectors.toList());

        execute(installation, targetDirectory, downloadOperations);
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
        for (Package pkg : operation.getResolveState().getInstalling()) {

            LOGGER.info("Installing {}", pkg.getName());

            final PackageOperationInstall installOp = new PackageOperationInstall(pkg);
            execute(installation, targetDirectory, installOp);
            LOGGER.info("Installation completed.");
        }
    }

    private void execute(Installation installation, Path targetDirectory, List<? extends PackageOperation> operations) throws IOException {
        for (PackageOperation operation : operations) {
            execute(installation, targetDirectory, operation);
        }
    }

    private void execute(Installation installation, Path targetDirectory, PackageOperation operation) throws IOException {
        final DefaultPackageOperationContext context = new DefaultPackageOperationContext(localPackageRepository, installation, targetDirectory,
                operation.getPackage());
        operation.execute(context);

        // save the generated log entries
        installationLogEntryRepository.save(context.getLog());
    }

    @Override
    public ProgressTaskDescription getDescription(Locale locale) {
        // FIXME
        return null;
    }

}

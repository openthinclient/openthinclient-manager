package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.db.Installation;
import org.openthinclient.util.dpkg.DPKGPackageInstallTask;
import org.openthinclient.util.dpkg.Package;
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

public class PackagesInstallOperation {

   private static final Logger LOGGER = LoggerFactory.getLogger(PackagesInstallOperation.class);

   private final Collection<org.openthinclient.util.dpkg.Package> packages;

   public PackagesInstallOperation(final Collection<Package> packages) {
      this.packages = packages;
   }

   public void execute(PackageManagerConfiguration configuration) throws PackageManagerException, IOException {
      LOGGER.info("Package installation started.");

      final Installation installation = new Installation();
      installation.setStart(LocalDateTime.now());

      // FIXME we should verify that the test install directory is actually empty at the moment.
      final Path testInstallDir = configuration.getTestinstallDir().toPath();

      final DefaultPackageOperationContext context = new DefaultPackageOperationContext(installation, testInstallDir);

      LOGGER.info("Phase 1: Installation into test-install directory ({})", testInstallDir);
      doInstall(configuration, context);

      LOGGER.info("Phase 2: Moving installed contents to the destination directory", configuration.getInstallDir());
      doMoveInstalledContents(configuration);

      installation.setEnd(LocalDateTime.now());

      LOGGER.info("Package installation completed.");
   }

   private void doMoveInstalledContents(final PackageManagerConfiguration configuration) throws IOException {

      final Path testInstallDir = configuration.getTestinstallDir().toPath().toAbsolutePath();
      final Path targetDir = configuration.getInstallDir().toPath().toAbsolutePath();

      Files.walkFileTree(testInstallDir, new FileVisitor<Path>() {

         @Override public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {

            // create the directory in our installation
            final Path relative = testInstallDir.relativize(dir);
            final Path target = targetDir.resolve(relative);

            Files.createDirectories(target);

            return FileVisitResult.CONTINUE;
         }

         @Override public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {

            final Path relative = testInstallDir.relativize(file);
            final Path target = targetDir.resolve(relative);

            Files.move(file, target);

            return FileVisitResult.CONTINUE;
         }

         @Override public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
            LOGGER.error("Failed to visit installed file {}", file);
            return FileVisitResult.CONTINUE;
         }

         @Override public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            // FIXME what should be done in case of exc != null
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
         }
      });

   }

   private void doInstall(final PackageManagerConfiguration configuration, final DefaultPackageOperationContext context) throws PackageManagerException {
      for(Package pkg : packages) {

         final Path localPackageFile = configuration.getArchivesDir().toPath().toAbsolutePath().resolve(pkg.getFilename());

         LOGGER.info("Installing {} ({})", pkg.getName(), localPackageFile);

         final DPKGPackageInstallTask task = new DPKGPackageInstallTask(pkg, localPackageFile);
         task.install(context);
      }
   }
}

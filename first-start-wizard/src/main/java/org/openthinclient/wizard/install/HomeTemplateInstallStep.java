package org.openthinclient.wizard.install;

import org.openthinclient.api.context.InstallContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALL_HOMETEMPLATEINSTALLSTEP_LABEL;

public class HomeTemplateInstallStep extends AbstractInstallStep {

  private static final Logger LOG = LoggerFactory.getLogger(HomeTemplateInstallStep.class);

  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    final Path targetDirectory = installContext.getManagerHome().getLocation().toPath();

    URI uri = getClass().getResource("/home_template").toURI();
    if (uri.getScheme().equals("jar")) {
      // there are two possible variants:
      // the path is directly within the jar. In that case there will be only two segments.
      // If library has been repackaged using spring boot repackage, there will be three segments
      // (the outer jar is only a container for all other libraries, required to run the application).
      final String[] segments = uri.toString().split("!");

      if (segments.length == 2) {
        // simple case, we're referencing the directory inside the main jar
        try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
          copyTemplate(fileSystem.getPath("/home_template"), targetDirectory);
        }
      } else {
        // open the filesystem for our main jar
        try (FileSystem intermediate = FileSystems.newFileSystem(URI.create(segments[0]), Collections.emptyMap())) {
          // now open the nested filesystem by copying the nested jar into a temporary file
          // this is due to the ZipFileSystem implementation, that does not support opening
          // ZipFileSystems within ZipFileSystems
          final Path nestedPath = intermediate.getPath(segments[1]);
          final Path tempFile = Files.createTempFile("otc-nested-", ".jar");
          Files.copy(nestedPath, tempFile, StandardCopyOption.REPLACE_EXISTING);
          try (FileSystem fileSystem = FileSystems.newFileSystem(tempFile, null)) {
            copyTemplate(fileSystem.getPath("/home_template"), targetDirectory);
          }
        }
      }
    } else {
      // When launching using an IDE, the files will not be packaged as a JAR. The classpath will contain
      // directories instead.
      copyTemplate(Paths.get(uri), targetDirectory);
    }
  }

  private void copyTemplate(Path templateBasePath, Path targetDirectory) throws IOException {
    Files.walk(templateBasePath, 200)
            // remove the template base directory
            .filter(path -> !path.equals(templateBasePath))
            .forEach(templatePath -> {

              final Path relativePath = templateBasePath.relativize(templatePath);
              final Path target = targetDirectory.resolve(relativePath.toString());

              if (Files.isDirectory(templatePath)) {
                LOG.info(String.format("[DIR] %-40s -> %-40s", relativePath, target));
                try {
                  Files.createDirectories(target);
                } catch (IOException e) {
                  throw new RuntimeException("Failed to create directory " + target, e);
                }
              } else {
                LOG.info(String.format("[CP ] %-40s -> %-40s", relativePath, target));
                try {
                  Files.copy(templatePath, target);
                } catch (IOException e) {
                  throw new RuntimeException("Failed to copy template file " + relativePath, e);
                }
              }

            });
  }

  @Override
  public String getName() {
    return  mc.getMessage(UI_FIRSTSTART_INSTALL_HOMETEMPLATEINSTALLSTEP_LABEL);
  }

  @Override
  public double getProgress() {
    return 1;
  }
}

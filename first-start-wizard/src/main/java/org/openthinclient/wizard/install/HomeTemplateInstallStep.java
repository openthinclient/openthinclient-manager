package org.openthinclient.wizard.install;

import org.openthinclient.api.context.InstallContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALL_HOMETEMPLATEINSTALLSTEP_LABEL;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class HomeTemplateInstallStep extends AbstractInstallStep {

  private static final Logger LOG = LoggerFactory.getLogger(HomeTemplateInstallStep.class);

  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    final Path targetDirectory = installContext.getManagerHome().getLocation().toPath();

    URI uri = getClass().getResource("/home_template").toURI();
    Path templateBasePath;
    if (uri.getScheme().equals("jar")) {
      FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
      templateBasePath = fileSystem.getPath("/home_template");
    } else {
      templateBasePath = Paths.get(uri);
    }
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
}

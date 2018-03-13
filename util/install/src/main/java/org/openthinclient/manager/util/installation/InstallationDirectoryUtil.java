package org.openthinclient.manager.util.installation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InstallationDirectoryUtil contains some installation related methods
 */
public class InstallationDirectoryUtil {

  private static final Logger LOG = LoggerFactory.getLogger(InstallationDirectoryUtil.class);
  private static final String INSTALL_FILE_NAME = ".installation.txt";

  /**
   * Delete alle entries form given directory
   * @param managerHomeDirectory the base directory
   */
  public static void cleanupManagerHomeDirectory(Path managerHomeDirectory) {
    Path installFile = Paths.get(managerHomeDirectory.toString(), INSTALL_FILE_NAME);
    if (Files.exists(installFile)) {
      LOG.info("Found existing installation file {}, delete manangerHome content and restart installation.", installFile);
      try {
        Files.walk(managerHomeDirectory)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
      } catch (IOException e) {
        LOG.error("Cannot cleanup manager-home directory '" + managerHomeDirectory.toAbsolutePath() + "' to prepare installation.", e);
      }
    }

  }

  /**
   * Removes the installation file
   * @param managerHomeDirectory the base directory
   */
  public static void removeInstallationFile(Path managerHomeDirectory) {

    Path installFile = Paths.get(managerHomeDirectory.toString(), INSTALL_FILE_NAME);
    if (Files.exists(installFile)) {
      LOG.info("Removing existing installation file {}", installFile);
      try {
        Files.delete(installFile);
      } catch (IOException e) {
        LOG.error("Cannot delete file " + installFile, e);
      }
    }
  }

  /**
   * Creates an installation file
   * @param managerHomeDirectory the base directory
   */
  public static void createInstallationProgressFile(Path managerHomeDirectory) {

    Path installProgress = Paths.get(managerHomeDirectory.toString(), INSTALL_FILE_NAME);
    LOG.debug("Creating new installation file");
    try {
      Files.createFile(installProgress);
    } catch (IOException e) {
      LOG.error("Cannot create file " + installProgress, e);
    }
  }

  /**
   * Append text to the installation file
   * @param managerHomeDirectory  the base directory
   * @param content text
   */
  public static void appendText(File managerHomeDirectory, String content) {
    Path installFile = Paths.get(managerHomeDirectory.getPath(), INSTALL_FILE_NAME);
    try {
      Files.write(installFile, Collections.singleton(content));
    } catch (IOException e) {
      LOG.error("Cannot write content '" + content + "' to file " + installFile, e);
    }
  }

  /**
   * Return true if an installlation file could be found
   * @param managerHomeDirectory  the base directory
   * @return
   */
  public static boolean existsInstallationProgressFile(File managerHomeDirectory) {
    return Files.exists(Paths.get(managerHomeDirectory.getPath(), INSTALL_FILE_NAME));
  }

  /**
   * Checks if the given director is empty, ignoring some special entries
   * @param directory  the base directory
   * @return
   */
  public static boolean isInstallationDirectoryEmpty(File directory) {
    return isInstallationDirectoryEmpty(directory, false);
  }

  public static boolean isInstallationDirectoryEmpty(File directory, boolean ignoreInstallFile) {
    final File[] contents = directory.listFiles(pathname -> {
      return
          // ignore typical MacOS directories
          !pathname.getName().equals(".DS_Store") &&
          // the installer will create a system property logging.file which will point to a file in the logs directory.
          !pathname.getName().equals("logs");
    });

    boolean existingInstallFile = Files.exists(Paths.get(directory.getPath(), INSTALL_FILE_NAME));

    if (ignoreInstallFile) {
      return contents != null && contents.length == 0;
    } else {
      return contents != null && contents.length == 0 && !existingInstallFile;
    }
  }

}

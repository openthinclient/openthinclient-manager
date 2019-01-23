package org.openthinclient.advisor.check;

import static org.openthinclient.advisor.AdvisorMessages.ADVISOR_CHECKMANAGERHOMEDIRECTORY_DESCRIPTION;
import static org.openthinclient.advisor.AdvisorMessages.ADVISOR_CHECKMANAGERHOMEDIRECTORY_TITLE;

import ch.qos.cal10n.MessageConveyor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;
import org.openthinclient.manager.util.installation.InstallationDirectoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckManagerHomeDirectory extends AbstractCheck<Boolean> {

  private static final Logger LOG = LoggerFactory.getLogger(CheckManagerHomeDirectory.class);

  private final File directory;

  public CheckManagerHomeDirectory(Locale locale, File directory) {
    super(new MessageConveyor(locale).getMessage(ADVISOR_CHECKMANAGERHOMEDIRECTORY_TITLE), 
          new MessageConveyor(locale).getMessage(ADVISOR_CHECKMANAGERHOMEDIRECTORY_DESCRIPTION));

    if (directory == null) {
      throw new IllegalArgumentException("directory must not be null");
    }
    this.directory = directory;
  }

  @Override
  protected CheckExecutionResult<Boolean> perform() {
    LOG.info("checking manager home directory '{}'", directory);


    if (directory.exists()) {
      if (!directory.isDirectory()) {
        // the path is pointing to a file instead of a directory
        return new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.FAILED, false);
      }

      // ensure that the specified directory is empty
      if (!InstallationDirectoryUtil.isInstallationDirectoryEmpty(directory, true)) {
        // the manager home directory is not empty
        return new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.FAILED, false);
      }

    } else {
      // the manager home directory doesn't exist. Try to create the directory
      if (!directory.mkdirs()) {
        // failed to create the manager home directory.
        return new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.FAILED, false);
      }
    }

    // we have a manager home directory. Now verify that the directory is writable.

    final File targetFile = new File(directory, "test.txt");
    try (final OutputStream out = new FileOutputStream(targetFile)) {
      out.write("TEST".getBytes());
    } catch (Exception e) {
      LOG.info("Creating a test file in the directory '{}' failed", directory);
      LOG.info("Failure reason.", e);
      return new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.FAILED, false);
    } finally {
      // try to clean up.
      targetFile.delete();
    }

    return new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.SUCCESS, true);
  }
}

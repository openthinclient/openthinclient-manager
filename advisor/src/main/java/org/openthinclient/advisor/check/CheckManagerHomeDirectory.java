package org.openthinclient.advisor.check;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class CheckManagerHomeDirectory extends AbstractCheck<Boolean> {

  private static final Logger LOG = LoggerFactory.getLogger(CheckManagerHomeDirectory.class);

  private final File directory;

  public CheckManagerHomeDirectory(File directory) {
    super("Check the manager home directory", "This check will verify that the given manager home directory is valid and writable.");

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
      final File[] contents = directory.listFiles(pathname -> {
        // ignore typical MacOS directories
        return !pathname.getName().equals(".DS_Store");
      });
      if (contents != null && contents.length > 0) {
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

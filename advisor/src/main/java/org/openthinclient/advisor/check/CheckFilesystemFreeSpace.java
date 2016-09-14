package org.openthinclient.advisor.check;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckFilesystemFreeSpace extends AbstractCheck<Boolean> {

  private static final Logger LOG = LoggerFactory.getLogger(CheckFilesystemFreeSpace.class);
  private final Path installDir;
  private final long minFreeSpace;
  
  /**
   * CheckFilesystemFreeSpace
   * @param installDir the Path for otc-install directory
   * @param minFreeSpace amount in megabytes
   */
  public CheckFilesystemFreeSpace(Path installDir, long minFreeSpace) {
    super("Check the file-system free space: minimum " + minFreeSpace + "Mb", "This check will verify that the file-system has engough free space for installation and runtime.");
    this.installDir  = installDir;
    this.minFreeSpace = minFreeSpace;
  }

  @Override
  protected CheckExecutionResult<Boolean> perform() {
    
    long freeSpace = installDir.getRoot().toFile().getFreeSpace();
    LOG.info("Free space for '" + installDir.getRoot() + "' is " + freeSpace + " bytes (" + (freeSpace/1024/1024) + "Mb)");
    if (freeSpace > minFreeSpace*1024*1024) {
      return new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.SUCCESS, true);
    }
    return new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.FAILED, false);
  }

}

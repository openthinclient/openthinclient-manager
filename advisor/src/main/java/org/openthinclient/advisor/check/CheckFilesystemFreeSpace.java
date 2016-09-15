package org.openthinclient.advisor.check;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.openthinclient.advisor.AdvisorParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckFilesystemFreeSpace extends AbstractCheck<Boolean> {

  public static final String MANAGER_HOME_DIRECTORY = "managerHomeInstallDir";
  
  private static final Logger LOG = LoggerFactory.getLogger(CheckFilesystemFreeSpace.class);
  private final long minFreeSpace;
  private final AdvisorParameter params;
  
  /**
   * CheckFilesystemFreeSpace
   * @param params the Path for otc-install directory will be obtained via {@link AdvisorParameter} 
   * @param minFreeSpace amount in megabytes
   */
  public CheckFilesystemFreeSpace(AdvisorParameter params, long minFreeSpace) {
    super(minFreeSpace > 0 ? "Check the file-system free space: minimum " + minFreeSpace + "Mb" : "Check the file-system free space skiped.", 
          "This check will verify that the file-system has engough free space for installation and runtime.");

    this.minFreeSpace = minFreeSpace;
    this.params = params;
  }

  @Override
  protected CheckExecutionResult<Boolean> perform() {
    
    final Path installDir = Paths.get(params.getStringParam(MANAGER_HOME_DIRECTORY));
    long freeSpace = installDir.getRoot().toFile().getFreeSpace();
    LOG.info("Free space for '" + installDir.getRoot() + "' is " + freeSpace + " bytes (" + (freeSpace/1024/1024) + "Mb)");
    
    if (minFreeSpace == 0) {
      return new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.WARNING, true);
    } else if (freeSpace > minFreeSpace*1024*1024) {
      return new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.SUCCESS, true);
    }
    return new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.FAILED, false);
  }

}

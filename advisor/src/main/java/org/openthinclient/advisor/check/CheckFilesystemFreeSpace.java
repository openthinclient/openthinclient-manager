package org.openthinclient.advisor.check;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Supplier;

import static org.openthinclient.advisor.AdvisorMessages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public class CheckFilesystemFreeSpace extends AbstractCheck<Boolean> {

  public static final String MANAGER_HOME_DIRECTORY = "managerHomeInstallDir";
  
  private static final Logger LOG = LoggerFactory.getLogger(CheckFilesystemFreeSpace.class);
  private final long minFreeSpace;
  private final Supplier<Path> pathSupplier;
  
  protected IMessageConveyor mc = new MessageConveyor(null);
  
  /**
   * CheckFilesystemFreeSpace
   * @param pathSupplier the Path for otc-install directory will be obtained via {@link AdvisorParameter} 
   * @param minFreeSpace amount in megabytes
   */
  public CheckFilesystemFreeSpace(Locale locale, Supplier<Path> pathSupplier, long minFreeSpace) {
    super(minFreeSpace > 0 ? new MessageConveyor(locale).getMessage(ADVISOR_CHECKFILESYSTEMFREESPACE_FREESPACE_MINIMUM, minFreeSpace) : new MessageConveyor(locale).getMessage(ADVISOR_CHECKFILESYSTEMFREESPACE_SKIPED), 
        new MessageConveyor(locale).getMessage(ADVISOR_CHECKFILESYSTEMFREESPACE_DESCRIPTION));

    this.minFreeSpace = minFreeSpace;
    this.pathSupplier = pathSupplier;
  }

  @Override
  protected CheckExecutionResult<Boolean> perform() {
    
    final Path installDir = this.pathSupplier.get(); 
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

package org.openthinclient.wizard.install;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractInstallStep {

  protected final Logger log = LoggerFactory.getLogger(getClass());
  private volatile InstallState installState = InstallState.PENDING;

  public InstallState getState() {
    return installState;
  }

  public final void execute(InstallContext installContext) {

    log.info("Starting install task");
    installState = InstallState.RUNNING;

    try {
      doExecute(installContext);
      log.info("Install task completed.");
      installState = InstallState.FINISHED;
      return;
    } catch (Exception e) {
      log.error("Install task failed.", e);
      installState = InstallState.FAILED;
      throw new RuntimeException(e);
    }
  }

  protected abstract void doExecute(InstallContext installContext) throws Exception;

}

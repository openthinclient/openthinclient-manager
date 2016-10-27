package org.openthinclient.wizard.install;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.UI;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public abstract class AbstractInstallStep {

  protected final Logger log = LoggerFactory.getLogger(getClass());
  private volatile InstallState installState = InstallState.PENDING;

  protected IMessageConveyor mc;
  
  public AbstractInstallStep() {
    Locale locale;
    if (UI.getCurrent() != null) {
       locale = UI.getCurrent().getLocale();
    } else {
       locale = Locale.getDefault();
    }
    mc = new MessageConveyor(locale);
  }
  
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

  public abstract String getName();
}

package org.openthinclient.wizard.install;

import org.openthinclient.api.context.InstallContext;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.syslogd.SyslogServiceConfiguration;
import org.openthinclient.tftp.TFTPServiceConfiguration;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALL_CONFIGURESYSLOGINSTALLSTEP_LABEL;

public class ConfigureSyslogInstallStep extends AbstractInstallStep {
  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    final ManagerHome managerHome = installContext.getManagerHome();
    final SyslogServiceConfiguration configuration = managerHome.getConfiguration(SyslogServiceConfiguration.class);

    configuration.setSyslogPort(514);
    
    // save the configuration
    managerHome.save(TFTPServiceConfiguration.class);
  }

  @Override
  public String getName() {
    return mc.getMessage(UI_FIRSTSTART_INSTALL_CONFIGURESYSLOGINSTALLSTEP_LABEL);
  }

  @Override
  public double getProgress() {
    return 1;
  }
}

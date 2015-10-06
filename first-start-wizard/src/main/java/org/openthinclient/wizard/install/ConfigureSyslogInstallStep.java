package org.openthinclient.wizard.install;

import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.syslogd.SyslogServiceConfiguration;
import org.openthinclient.tftp.TFTPServiceConfiguration;

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
    return "Configure the TFTP exports";
  }
}

package org.openthinclient.wizard.install;

import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;

public class BootstrapLDAPInstallStep extends AbstractInstallStep {
  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    final ManagerHome managerHome = installContext.getManagerHome();
    final DirectoryServiceConfiguration directoryServiceConfiguration = managerHome.getConfiguration(DirectoryServiceConfiguration.class);

    // we're all ok with the defaults

    log.info("Saving the default ldap configuration to the manager home");
    managerHome.save(DirectoryServiceConfiguration.class);


    log.info("Starting the embedded LDAP server and bootstrapping the configuration");
    final DirectoryService directoryService = new DirectoryService();
    directoryService.setConfiguration(directoryServiceConfiguration);
    directoryService.startService();

    log.info("Stopping the embedded LDAP server.");
    directoryService.stopService();
  }

  @Override
  public String getName() {
    return "Initial LDAP configuration";
  }
}

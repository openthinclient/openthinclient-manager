package org.openthinclient.wizard.install;

import org.openthinclient.api.context.InstallContext;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.ServerIDFactory;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.ManagerHomeMetadata;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.model.NetworkConfigurationModel;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALL_PREPAREMANAGERHOMEINSTALLSTEP_LABEL;

public class PrepareManagerHomeInstallStep extends AbstractInstallStep {
  private final ManagerHomeFactory managerHomeFactory;
  private final NetworkConfigurationModel networkConfigurationModel;

  public PrepareManagerHomeInstallStep(ManagerHomeFactory managerHomeFactory, NetworkConfigurationModel networkConfigurationModel) {
    this.managerHomeFactory = managerHomeFactory;
    this.networkConfigurationModel = networkConfigurationModel;
  }

  @Override
  public String getName() {
    return mc.getMessage(UI_FIRSTSTART_INSTALL_PREPAREMANAGERHOMEINSTALLSTEP_LABEL);
  }

  @Override
  protected void doExecute(InstallContext installContext) throws Exception {
    // initialize the manager home directory
    log.info("Preparing the manager home directory: " + managerHomeFactory.getManagerHomeDirectory().getAbsolutePath());
    managerHomeFactory.getManagerHomeDirectory().mkdirs();
    final ManagerHome managerHome = managerHomeFactory.create();

    // generating and persisting a server id
    final ManagerHomeMetadata metadata = managerHome.getMetadata();
    metadata.setServerID(ServerIDFactory.create());
    metadata.save();

    log.info("\n#########################################################\n" +
            "# Server ID: " + metadata.getServerID() + "\n" +
            "#########################################################");

    // ensure that some configurations are known and will be stored
    log.info("Performing the minimum system configuration.");
    final PackageManagerConfiguration packageManagerConfiguration = managerHome.getConfiguration(PackageManagerConfiguration.class);

    if (networkConfigurationModel.getProxyConnectionProperty()) {
      log.info("Setting up the proxy configuration");
      packageManagerConfiguration.setProxyConfiguration(networkConfigurationModel.getProxyConfiguration());
      packageManagerConfiguration.getProxyConfiguration().setEnabled(true);
    }
    // FIXME there should be a flag indicating that the manager will be running in an offline mode

    managerHome.saveAll();

    installContext.setManagerHome(managerHome);
  }
}

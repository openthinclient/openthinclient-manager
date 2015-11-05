package org.openthinclient.wizard.install;

import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.SourcesListWriter;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.model.NetworkConfigurationModel;

import java.io.FileOutputStream;

public class PrepareManagerHomeInstallStep extends AbstractInstallStep {
  private final ManagerHomeFactory managerHomeFactory;
  private final InstallableDistribution installableDistribution;
  private final NetworkConfigurationModel networkConfigurationModel;

  public PrepareManagerHomeInstallStep(ManagerHomeFactory managerHomeFactory, InstallableDistribution installableDistribution, NetworkConfigurationModel networkConfigurationModel) {
    this.managerHomeFactory = managerHomeFactory;
    this.installableDistribution = installableDistribution;
    this.networkConfigurationModel = networkConfigurationModel;
  }

  @Override
  public String getName() {
    return "Prepare the manager home directory";
  }

  @Override
  protected void doExecute(InstallContext installContext) throws Exception {
    // initialize the manager home directory
    log.info("Preparing the manager home directory: " + managerHomeFactory.getManagerHomeDirectory().getAbsolutePath());
    managerHomeFactory.getManagerHomeDirectory().mkdirs();
    final ManagerHome managerHome = managerHomeFactory.create();

    // ensure that some configurations are known and will be stored
    log.info("Performing the minimum system configuration.");
    final PackageManagerConfiguration packageManagerConfiguration = managerHome.getConfiguration(PackageManagerConfiguration.class);

    if (networkConfigurationModel.getProxyConnectionProperty().getValue()) {
      log.info("Setting up the proxy configuration");
      packageManagerConfiguration.setProxyConfiguration(networkConfigurationModel.getProxyConfiguration());
      packageManagerConfiguration.getProxyConfiguration().setEnabled(true);
    }
    // FIXME there should be a flag indicating that the manager will be running in an offline mode

    managerHome.saveAll();


    log.info("Writing the sources.list for the selected distribution");
    final SourcesListWriter writer = new SourcesListWriter();
    try (final FileOutputStream out = new FileOutputStream(packageManagerConfiguration.getSourcesList())) {
      writer.write(installableDistribution.getSourcesList(), out);
    }

    installContext.setManagerHome(managerHome);
  }
}

package org.openthinclient.wizard.model;

import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.inventory.SystemInventory;

public class SystemSetupModel {
  private final NetworkConfigurationModel networkConfigurationModel;
  private final CheckEnvironmentModel checkEnvironmentModel;

  public SystemSetupModel(SystemInventory systemInventory, CheckExecutionEngine checkExecutionEngine) {

    this.networkConfigurationModel = new NetworkConfigurationModel();
    this.checkEnvironmentModel = new CheckEnvironmentModel(systemInventory, checkExecutionEngine);

  }

  public NetworkConfigurationModel getNetworkConfigurationModel() {
    return networkConfigurationModel;
  }

  public CheckEnvironmentModel getCheckEnvironmentModel() {
    return checkEnvironmentModel;
  }
}

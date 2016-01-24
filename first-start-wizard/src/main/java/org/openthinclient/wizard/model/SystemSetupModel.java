package org.openthinclient.wizard.model;

import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.inventory.SystemInventory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.AsyncListenableTaskExecutor;

public class SystemSetupModel {
  private final NetworkConfigurationModel networkConfigurationModel;
  private final CheckEnvironmentModel checkEnvironmentModel;
  private final ManagerHomeModel managerHomeModel;
  private final InstallModel installModel;
  private final DirectoryModel directoryModel;
  private final DatabaseModel databaseModel;

  public SystemSetupModel(SystemInventory systemInventory, CheckExecutionEngine checkExecutionEngine, ApplicationContext applicationContext, AsyncListenableTaskExecutor taskExecutor) {

    this.networkConfigurationModel = new NetworkConfigurationModel();
    this.checkEnvironmentModel = new CheckEnvironmentModel(systemInventory, checkExecutionEngine);
    this.managerHomeModel = new ManagerHomeModel(checkExecutionEngine);
    this.directoryModel = new DirectoryModel();
    this.databaseModel = new DatabaseModel();
    this.installModel = new InstallModel(taskExecutor, directoryModel, networkConfigurationModel, databaseModel);
  }

  public NetworkConfigurationModel getNetworkConfigurationModel() {
    return networkConfigurationModel;
  }

  public CheckEnvironmentModel getCheckEnvironmentModel() {
    return checkEnvironmentModel;
  }

  public ManagerHomeModel getManagerHomeModel() {
    return managerHomeModel;
  }

  public InstallModel getInstallModel() {
    return installModel;
  }

  public DirectoryModel getDirectoryModel() {
    return directoryModel;
  }

  public DatabaseModel getDatabaseModel() {
    return databaseModel;
  }
}

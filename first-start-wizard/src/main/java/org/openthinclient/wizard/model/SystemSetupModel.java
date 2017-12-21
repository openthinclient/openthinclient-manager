package org.openthinclient.wizard.model;

import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.inventory.SystemInventory;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.AsyncListenableTaskExecutor;

public class SystemSetupModel {
  private final NetworkConfigurationModel networkConfigurationModel;
  private final CheckEnvironmentModel checkEnvironmentModel;
  private final ManagerHomeModel managerHomeModel;
  private final InstallModel installModel;
  private final DirectoryModel directoryModel;
  private final DatabaseModel databaseModel;
  private final ManagerHomeFactory factory;

  public SystemSetupModel(ManagerHomeFactory factory, SystemInventory systemInventory, CheckExecutionEngine checkExecutionEngine,
                          ApplicationContext applicationContext, AsyncListenableTaskExecutor taskExecutor, int installationFreespaceMinimum) {

    this.factory = factory;
    this.networkConfigurationModel = new NetworkConfigurationModel();
    this.managerHomeModel = new ManagerHomeModel(factory, checkExecutionEngine);
    this.checkEnvironmentModel = new CheckEnvironmentModel(systemInventory, checkExecutionEngine, managerHomeModel, installationFreespaceMinimum);
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

/**
 * @return the factory
 */
public ManagerHomeFactory getFactory() {
   return factory;
}
}

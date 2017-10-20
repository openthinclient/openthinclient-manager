package org.openthinclient.wizard.model;

import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.api.distributions.InstallableDistributions;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.install.InstallSystemTask;
import org.springframework.core.task.AsyncListenableTaskExecutor;

import java.util.ArrayList;
import java.util.List;

public class InstallModel {

  public static final InstallableDistribution DEFAULT_DISTRIBUTION = InstallableDistributions.getPreferredDistribution();

    private final List<InstallableDistribution> installableDistributions;
    private final AsyncListenableTaskExecutor taskExecutor;
    private final DirectoryModel directoryModel;
    private final NetworkConfigurationModel networkConfigurationModel;
    private final DatabaseModel databaseModel;
    private volatile InstallSystemTask installSystemTask;

    public InstallModel(AsyncListenableTaskExecutor taskExecutor, DirectoryModel directoryModel, NetworkConfigurationModel networkConfigurationModel,
                        DatabaseModel databaseModel) {
        this.taskExecutor = taskExecutor;
        this.directoryModel = directoryModel;
        this.networkConfigurationModel = networkConfigurationModel;
        this.databaseModel = databaseModel;
        installableDistributions = new ArrayList<>();
        installableDistributions.add(DEFAULT_DISTRIBUTION);
    }

    public List<InstallableDistribution> getInstallableDistributions() {
        return installableDistributions;
    }

    public boolean isInstallInProgress() {
        return installSystemTask != null;
    }

    public InstallSystemTask installSystem(ManagerHomeFactory factory, InstallableDistribution installableDistribution) {
        installSystemTask = new InstallSystemTask(factory, installableDistribution, directoryModel, networkConfigurationModel, databaseModel);
        taskExecutor.submitListenable(installSystemTask);

        return installSystemTask;
    }

    public InstallSystemTask getInstallSystemTask() {
        return installSystemTask;
    }
}

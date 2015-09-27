package org.openthinclient.wizard.install;

import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.model.DirectoryModel;
import org.openthinclient.wizard.model.NetworkConfigurationModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class InstallSystemTask implements Callable<Boolean> {

  private final List<AbstractInstallStep> steps;
  private volatile InstallState installState = InstallState.PENDING;

  public InstallSystemTask(ManagerHomeFactory managerHomeFactory, InstallableDistribution installableDistribution, DirectoryModel directoryModel, NetworkConfigurationModel networkConfigurationModel) {

    final ArrayList<AbstractInstallStep> mutableSteps = new ArrayList<>();

    mutableSteps.add(new PrepareManagerHomeInstallStep(managerHomeFactory, installableDistribution, networkConfigurationModel));
    mutableSteps.add(new HomeTemplateInstallStep());
    mutableSteps.add(new CreatePackageManagerUpdatedPackageListInstallStep());
    mutableSteps.add(new RequiredPackagesInstallStep(installableDistribution));
    mutableSteps.add(new ConfigureTFTPInstallStep());
    mutableSteps.add(new ConfigureNFSInstallStep());
    mutableSteps.add(new BootstrapLDAPInstallStep(directoryModel));

    steps = Collections.unmodifiableList(mutableSteps);

  }

  public InstallState getInstallState() {
    return installState;
  }

  public List<AbstractInstallStep> getSteps() {
    return steps;
  }

  @Override
  public Boolean call() throws Exception {

    installState = InstallState.RUNNING;

    final InstallContext installContext = new InstallContext();

    try {
      steps.forEach(step -> step.execute(installContext));
    } catch (Exception e) {
      installState = InstallState.FAILED;
      throw e;
    }

    installState = InstallState.FINISHED;
    return true;
  }

}

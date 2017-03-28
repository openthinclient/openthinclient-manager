package org.openthinclient.wizard.install;

import org.openthinclient.api.context.InstallContext;
import org.openthinclient.api.distributions.ImportableProfileProvider;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.model.DatabaseModel;
import org.openthinclient.wizard.model.DirectoryModel;
import org.openthinclient.wizard.model.NetworkConfigurationModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class InstallSystemTask implements Callable<Boolean> {

  private final List<AbstractInstallStep> steps;
  private volatile InstallState installState = InstallState.PENDING;

  public InstallSystemTask(ManagerHomeFactory managerHomeFactory, InstallableDistribution installableDistribution, DirectoryModel directoryModel,
                           NetworkConfigurationModel networkConfigurationModel, DatabaseModel databaseModel) {

    final ArrayList<AbstractInstallStep> mutableSteps = new ArrayList<>();

    mutableSteps.add(new PrepareManagerHomeInstallStep(managerHomeFactory, networkConfigurationModel));
    mutableSteps.add(new HomeTemplateInstallStep());
    mutableSteps.add(new PrepareDatabaseInstallStep(databaseModel));
    mutableSteps.add(new PackageManagerUpdatedPackageListInstallStep(installableDistribution));
    mutableSteps.add(new RequiredPackagesInstallStep(installableDistribution));
    mutableSteps.add(new ConfigureTFTPInstallStep());
    mutableSteps.add(new ConfigureNFSInstallStep());
    mutableSteps.add(new ConfigureSyslogInstallStep());
    mutableSteps.add(new BootstrapLDAPInstallStep(directoryModel, installableDistribution, new ImportableProfileProvider(installableDistribution.getParent().getBaseURI())));
    mutableSteps.add(new FinalizeInstallationStep());

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

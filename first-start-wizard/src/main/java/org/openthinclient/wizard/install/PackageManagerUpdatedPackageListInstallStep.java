package org.openthinclient.wizard.install;

import org.openthinclient.api.context.InstallContext;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.progress.ListenableProgressFuture;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALL_PACKAGEMANAGERUPDATEDPACKAGELISTINSTALLSTEP_LABEL;

public class PackageManagerUpdatedPackageListInstallStep extends AbstractInstallStep {

  private final InstallableDistribution distribution;

  public PackageManagerUpdatedPackageListInstallStep(InstallableDistribution distribution) {this.distribution = distribution;}

  @Override
  public String getName() {
    return  mc.getMessage(UI_FIRSTSTART_INSTALL_PACKAGEMANAGERUPDATEDPACKAGELISTINSTALLSTEP_LABEL);
  }

  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    log.info("configuring the sources list");

    installContext.getPackageManager().saveSources(distribution.getSourcesList().getSources());

    log.info("Downloading the latest packages list");
    // download the packages.gz and update our local database
    ListenableProgressFuture<PackageListUpdateReport> updateCacheDB = installContext.getPackageManager().updateCacheDB();
    updateCacheDB.get();

  }
}

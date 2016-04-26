package org.openthinclient.wizard.install;

import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;

public class PackageManagerUpdatedPackageListInstallStep extends AbstractInstallStep {

  private final InstallableDistribution distribution;

  public PackageManagerUpdatedPackageListInstallStep(InstallableDistribution distribution) {this.distribution = distribution;}

  @Override
  public String getName() {
    return "Download the latest packages lists";
  }

  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    log.info("configuring the sources list");

    installContext.getPackageManager().getSourceRepository().save(distribution.getSourcesList().getSources());

    log.info("Downloading the latest packages list");
    // download the packages.gz and update our local database
    ListenableProgressFuture<PackageListUpdateReport> updateCacheDB = installContext.getPackageManager().updateCacheDB();
    updateCacheDB.get();

  }
}

package org.openthinclient.wizard.install;

public class PackageManagerUpdatedPackageListInstallStep extends AbstractInstallStep {

  @Override
  public String getName() {
    return "Download the latest packages lists";
  }

  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    log.info("Downloading the latest packages list");
    // download the packages.gz and update our local database
    installContext.getPackageManager().updateCacheDB();


  }
}

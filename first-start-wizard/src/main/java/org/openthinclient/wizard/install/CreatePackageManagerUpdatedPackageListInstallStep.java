package org.openthinclient.wizard.install;

import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.util.dpkg.DPKGPackageManager;

public class CreatePackageManagerUpdatedPackageListInstallStep extends AbstractInstallStep {
  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    final PackageManagerConfiguration packageManagerConfiguration = installContext.getManagerHome().getConfiguration(PackageManagerConfiguration.class);

    // create a new package manager instance
    final DPKGPackageManager packageManager = PackageManagerFactory.createPackageManager(packageManagerConfiguration);

    installContext.setPackageManager(packageManager);

    log.info("Downloading the latest packages list");
    // download the packages.gz and update our local database
    packageManager.updateCacheDB();


  }
}

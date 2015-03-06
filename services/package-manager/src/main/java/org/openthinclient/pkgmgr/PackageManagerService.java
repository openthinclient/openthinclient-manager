package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.impl.PackageManagerImpl;
import org.openthinclient.service.common.Service;
import org.openthinclient.util.dpkg.DPKGPackageManager;

public class PackageManagerService implements Service<PackageManagerConfiguration> {

  private volatile boolean running;
  private PackageManagerConfiguration configuration;
  private PackageManager packageManager;

  public PackageManager getPackageManager() {
    if (!running) {
      throw new IllegalStateException("package manager service is not running");
    }

    return this.packageManager;
  }

  @Override
  public void setConfiguration(PackageManagerConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public PackageManagerConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public Class<PackageManagerConfiguration> getConfigurationClass() {
    return PackageManagerConfiguration.class;
  }

  @Override
  public void startService() throws Exception {
    running = true;

    final DPKGPackageManager dpkgPackageManager = PackageManagerFactory.createPackageManager(configuration);
    packageManager = new PackageManagerImpl(dpkgPackageManager);

  }

  @Override
  public void stopService() throws Exception {
    running = false;

    this.packageManager.close();

  }
}

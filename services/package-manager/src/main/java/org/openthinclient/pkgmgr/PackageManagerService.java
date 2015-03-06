package org.openthinclient.pkgmgr;

import org.openthinclient.service.common.Service;
import org.openthinclient.util.dpkg.DPKGPackageManager;

public class PackageManagerService implements Service<PackageManagerConfiguration> {

  private volatile boolean running;
  private PackageManagerConfiguration configuration;
  private DPKGPackageManager packageManager;

  public PackageManager getPackageManager() {
    if (!running) {
      throw new IllegalStateException("package manager service is not running");
    }

    if (packageManager == null)
      this.packageManager = PackageManagerFactory.createPackageManager(configuration);
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
  }

  @Override
  public void stopService() throws Exception {
    running = false;
  }
}

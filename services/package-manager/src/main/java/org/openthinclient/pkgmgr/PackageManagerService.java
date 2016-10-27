package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.impl.PackageManagerImpl;
import org.openthinclient.service.common.Service;
import org.openthinclient.service.nfs.NFS;

public class PackageManagerService implements Service<PackageManagerConfiguration> {

  private final NFS nfs;
  private final PackageManagerFactory packageManagerFactory;
  private volatile boolean running;
  private PackageManagerConfiguration configuration;
  private PackageManager packageManager;

  public PackageManagerService(NFS nfs, PackageManagerFactory packageManagerFactory) {
    if (nfs == null) {
      throw new IllegalArgumentException("nfs must not be null");
    }
    this.nfs = nfs;
    this.packageManagerFactory = packageManagerFactory;
  }

  public PackageManager getPackageManager() {
    if (!running) {
      throw new IllegalStateException("package manager service is not running");
    }

    return this.packageManager;
  }

  @Override
  public PackageManagerConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public void setConfiguration(PackageManagerConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public Class<PackageManagerConfiguration> getConfigurationClass() {
    return PackageManagerConfiguration.class;
  }

  @Override
  public void startService() throws Exception {
    running = true;

    final PackageManager dpkgPackageManager = packageManagerFactory.createPackageManager(configuration);
    packageManager = new PackageManagerImpl(dpkgPackageManager, nfs);

  }

  @Override
  public void stopService() throws Exception {
    running = false;

    this.packageManager.close();

  }
}

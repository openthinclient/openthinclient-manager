package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.impl.PackageManagerImpl;
import org.openthinclient.service.common.Service;
import org.openthinclient.service.nfs.NFS;
import org.openthinclient.util.dpkg.DPKGPackageManager;

public class PackageManagerService implements Service<PackageManagerConfiguration> {

  private final NFS nfs;
  private volatile boolean running;
  private PackageManagerConfiguration configuration;
  private PackageManager packageManager;

  public PackageManagerService(NFS nfs) {
    if (nfs == null) {
      throw new IllegalArgumentException("nfs must not be null");
    }
    this.nfs = nfs;
  }

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
    packageManager = new PackageManagerImpl(dpkgPackageManager, nfs);

  }

  @Override
  public void stopService() throws Exception {
    running = false;

    this.packageManager.close();

  }
}

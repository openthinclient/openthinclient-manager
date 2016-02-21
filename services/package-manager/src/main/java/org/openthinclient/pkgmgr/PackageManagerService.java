package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.impl.PackageManagerImpl;
import org.openthinclient.service.common.Service;
import org.openthinclient.service.nfs.NFS;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackageClasses = {SourceRepository.class})
public class PackageManagerService implements Service<PackageManagerConfiguration> {

  private final NFS nfs;
  private final SourceRepository sourceRepository;
  private final PackageRepository packageRepository;
  private volatile boolean running;
  private PackageManagerConfiguration configuration;
  private PackageManager packageManager;

  public PackageManagerService(NFS nfs, SourceRepository sourceRepository, PackageRepository packageRepository) {
    this.sourceRepository = sourceRepository;
    this.packageRepository = packageRepository;
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

    final DPKGPackageManager dpkgPackageManager = PackageManagerFactory.createPackageManager(configuration, sourceRepository, packageRepository);
    packageManager = new PackageManagerImpl(dpkgPackageManager, nfs);

  }

  @Override
  public void stopService() throws Exception {
    running = false;

    this.packageManager.close();

  }
}

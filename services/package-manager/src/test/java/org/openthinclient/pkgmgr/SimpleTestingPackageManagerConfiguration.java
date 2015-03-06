package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.impl.PackageManagerImpl;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimpleTestingPackageManagerConfiguration {

  @Bean
  public DPKGPackageManager dpkgPackageManager(PackageManagerConfiguration configuration) {
    return PackageManagerFactory.createPackageManager(configuration);
  }

  @Bean(destroyMethod = "close")
  public PackageManagerImpl packageManagerImpl(DPKGPackageManager dpkgPackageManager) {
    return new PackageManagerImpl(dpkgPackageManager);
  }

}

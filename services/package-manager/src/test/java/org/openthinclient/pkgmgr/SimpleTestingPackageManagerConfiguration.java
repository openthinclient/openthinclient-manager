package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.impl.PackageManagerImpl;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimpleTestingPackageManagerConfiguration {

  @Bean
  public PackageManager dpkgPackageManager(PackageManagerConfiguration configuration) {
    return new PackageManagerFactory(null, null, null, null, null).createPackageManager(configuration);
  }

  @Bean(destroyMethod = "close")
  public PackageManagerImpl packageManagerImpl(DPKGPackageManager dpkgPackageManager) {
    // FIXME is there a way to have the nfs server optionally passed?
    return new PackageManagerImpl(dpkgPackageManager, null);
  }

}

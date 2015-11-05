package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerService;
import org.openthinclient.service.nfs.NFS;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class PackageManagerConfiguration {

  @Bean
  @Scope(value = "singleton")
  /* default */ PackageManagerService packageManagerService(NFS nfs) {
    return new PackageManagerService(nfs);
  }

  @Bean
  public PackageManager packageManager(PackageManagerService packageManagerService) {
    return packageManagerService.getPackageManager();
  }

}
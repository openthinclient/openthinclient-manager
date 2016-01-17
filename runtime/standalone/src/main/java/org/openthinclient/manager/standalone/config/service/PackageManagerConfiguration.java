package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerService;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.spring.PackageManagerRepositoryConfiguration;
import org.openthinclient.service.nfs.NFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@Configuration
@Import(PackageManagerRepositoryConfiguration.class)
public class PackageManagerConfiguration {

  @Autowired
  SourceRepository sourceRepository;

  @Bean
  @Scope(value = "singleton")
  /* default */ PackageManagerService packageManagerService(NFS nfs) {
    return new PackageManagerService(nfs, sourceRepository);
  }

  @Bean
  public PackageManager packageManager(PackageManagerService packageManagerService) {
    return packageManagerService.getPackageManager();
  }

}

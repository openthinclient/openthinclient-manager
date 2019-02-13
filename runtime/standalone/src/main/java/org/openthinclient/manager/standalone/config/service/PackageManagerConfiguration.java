package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.PackageManagerService;
import org.openthinclient.pkgmgr.spring.PackageManagerExecutionEngineConfiguration;
import org.openthinclient.pkgmgr.spring.PackageManagerFactoryConfiguration;
import org.openthinclient.pkgmgr.spring.PackageManagerRepositoryConfiguration;
import org.openthinclient.service.nfs.NFS;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

@Configuration
@Import({PackageManagerRepositoryConfiguration.class, PackageManagerExecutionEngineConfiguration.class, PackageManagerFactoryConfiguration.class})
public class PackageManagerConfiguration {

  //   @Bean
  //   @Scope(value = "singleton")
  // [> default <] PackageManagerService packageManagerService(NFS nfs, PackageManagerFactory packageManagerFactory) {
  //       return new PackageManagerService(nfs, packageManagerFactory);
  //   }

    @Bean
    @Scope(value = "prototype", proxyMode = ScopedProxyMode.INTERFACES)
    public PackageManager packageManager(PackageManagerService packageManagerService) {
        return packageManagerService.getPackageManager();
    }

}

package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerService;
import org.openthinclient.pkgmgr.impl.PackageManagerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;

@Configuration
public class PackageManagerConfiguration {

  @Bean
  @Scope(value = "singleton")
  /* default */ PackageManagerService packageManagerService() {
    return new PackageManagerService();
  }


  @Bean(name = "/service/httpinvoker/package-manager")
  public HttpInvokerServiceExporter packageManagerService(PackageManager packageManager) {
    final HttpInvokerServiceExporter serviceExporter = new HttpInvokerServiceExporter();
    serviceExporter.setService(packageManager);
    serviceExporter.setServiceInterface(PackageManager.class);
    return serviceExporter;
  }

}

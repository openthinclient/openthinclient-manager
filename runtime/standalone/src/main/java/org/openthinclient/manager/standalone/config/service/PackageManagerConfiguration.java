package org.openthinclient.manager.standalone.config.service;

import java.util.HashMap;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerService;
import org.openthinclient.pkgmgr.impl.PackageManagerImpl;
import org.openthinclient.service.nfs.NFS;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.remoting.httpinvoker.SimpleHttpInvokerServiceExporter;
import org.springframework.remoting.support.SimpleHttpServerFactoryBean;

import com.sun.net.httpserver.HttpHandler;

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

  @Bean(name = "/service/httpinvoker/package-manager")
  public HttpInvokerServiceExporter httpInvokerPackageManagerService(PackageManager packageManager) {
    final HttpInvokerServiceExporter serviceExporter = new HttpInvokerServiceExporter();
    serviceExporter.setService(packageManager);
    serviceExporter.setServiceInterface(PackageManager.class);
    return serviceExporter;
  }

  
  //
  // ------- Server side configuration -------
  //
  @Bean
  public SimpleHttpInvokerServiceExporter packageManagerServiceExporter(PackageManagerImpl packageManager) {
    final SimpleHttpInvokerServiceExporter exporter = new SimpleHttpInvokerServiceExporter();
    exporter.setServiceInterface(PackageManager.class);
    exporter.setService(packageManager);
    return exporter;
  }

  @Bean
  public SimpleHttpServerFactoryBean httpServer(SimpleHttpInvokerServiceExporter packageManagerServiceExporter) {
    final SimpleHttpServerFactoryBean httpServer = new SimpleHttpServerFactoryBean();
    final HashMap<String, HttpHandler> contexts = new HashMap<>();
    contexts.put("/PackageManagerService", packageManagerServiceExporter);
    httpServer.setContexts(contexts);
    httpServer.setPort(8087);
    return httpServer;
  }
  
}

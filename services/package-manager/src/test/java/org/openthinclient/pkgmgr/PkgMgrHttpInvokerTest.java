package org.openthinclient.pkgmgr;

import com.sun.net.httpserver.HttpHandler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.db.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.remoting.httpinvoker.SimpleHttpInvokerServiceExporter;
import org.springframework.remoting.support.SimpleHttpServerFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;
import java.util.HashMap;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PkgMgrHttpInvokerTest.HttpInvokerConfiguration.class)
public class PkgMgrHttpInvokerTest {

  private static final Logger logger = LoggerFactory.getLogger(PkgMgrHttpInvokerTest.class);
  @Autowired
  @Qualifier("packageManagerService")
  private PackageManager packageManagerService;

  @Test
  public void testFreeDiskSpace() throws PackageManagerException {
    long diskSpace = packageManagerService.getFreeDiskSpace();
    assertNotNull(diskSpace);
    logger.debug("FreeDiskSpace: " + diskSpace);
  }

  @Test
  public void testInstalledPackages() throws PackageManagerException {
    Collection<Package> packages = packageManagerService.getInstalledPackages();
    assertNotNull(packages);
    logger.debug("InstalledPackages: " + packages);
  }

  @Configuration
  @Import({SimpleTargetDirectoryPackageManagerConfiguration.class})
  public static class HttpInvokerConfiguration {

    @Autowired
    SimpleTargetDirectoryPackageManagerConfiguration packageManagerConfiguration;
    @Autowired
    PackageManagerFactory packageManagerFactory;
    //
    // ------- Server side configuration -------
    //
    @Bean
    public SimpleHttpInvokerServiceExporter packageManagerServiceExporter() throws Exception {
      final SimpleHttpInvokerServiceExporter exporter = new SimpleHttpInvokerServiceExporter();
      exporter.setServiceInterface(PackageManager.class);
      exporter.setService(packageManagerConfiguration.packageManager(packageManagerFactory));
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

    //
    // ------- Client side configuration -------
    //
    @Bean(name="packageManagerService")
    public FactoryBean<Object> packageManagerService() {
      final HttpInvokerProxyFactoryBean invokerProxyFactoryBean = new HttpInvokerProxyFactoryBean();
      invokerProxyFactoryBean.setServiceInterface(PackageManager.class);
      invokerProxyFactoryBean.setServiceUrl("http://localhost:8087/PackageManagerService");
      return invokerProxyFactoryBean;
    }

  }

}

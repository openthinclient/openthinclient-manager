package org.openthinclient.console.configuration;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.services.Dhcp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

import java.net.URL;

@Configuration
@ComponentScan(basePackages="org.openthinclient.console")
public class HttpInvokerConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(HttpInvokerConfiguration.class);
  public static final String PROPERTY_CODEBASE = System.getProperty("ThinClientManager.server.Codebase");

  @Bean
  public URL managerCodeBaseURL() throws Exception {

    try {
      final javax.jnlp.BasicService basicService =
              (javax.jnlp.BasicService) javax.jnlp.ServiceManager.
                      lookup("javax.jnlp.BasicService");
      return basicService.getCodeBase();
    } catch (Exception e) {
      LOG.warn("Failed to determine the codebase using the javax.jnlp.BasicService", e);
      // fallback. Try to get the codebase URL from the
      final String codebaseProperty = PROPERTY_CODEBASE;
      if (codebaseProperty != null) {
        LOG.info("Using the system property '{}'. Value: '{}'", PROPERTY_CODEBASE, codebaseProperty);

        return new URL(codebaseProperty);
      }

      // last resort, mostly useful for the development time localhost:

      LOG.warn("Falling back to localhost as the codebase URL");
      return new URL("http://localhost:8080");
    }

  }

    //
    // ------- Client side configuration -------
    //
    @Bean
    @Lazy
    public FactoryBean<Object> packageManagerService() throws Exception {
      final HttpInvokerProxyFactoryBean invokerProxyFactoryBean = new HttpInvokerProxyFactoryBean();
      invokerProxyFactoryBean.setServiceInterface(PackageManager.class);
      invokerProxyFactoryBean.setServiceUrl(new URL(managerCodeBaseURL(), "/service/httpinvoker/package-manager").toExternalForm());
      return invokerProxyFactoryBean;
    }
    
    @Bean
    @Lazy
    public FactoryBean<Object> dhcpService() throws Exception {
      final HttpInvokerProxyFactoryBean invokerProxyFactoryBean = new HttpInvokerProxyFactoryBean();
      invokerProxyFactoryBean.setServiceInterface(Dhcp.class);
      invokerProxyFactoryBean.setServiceUrl(new URL(managerCodeBaseURL(), "/service/httpinvoker/dhcp").toExternalForm());
//      invokerProxyFactoryBean.setHttpInvokerRequestExecutor(httpInvokerRequestExecutor); HttpComponentsHttpInvokerRequestExecutor
      return invokerProxyFactoryBean;
    }
    
}

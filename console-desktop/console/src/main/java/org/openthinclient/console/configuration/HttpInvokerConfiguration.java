package org.openthinclient.console.configuration;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.services.Dhcp;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

import java.net.URL;

@Configuration
@ComponentScan(basePackages="org.openthinclient.console")
public class HttpInvokerConfiguration {

  @Autowired
  Environment environment;

  @Bean
  public URL managerCodeBaseURL() throws Exception {

    final String serverName = environment.getProperty("manager.server.name");

    return new URL("http://" + serverName + ":8080");
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

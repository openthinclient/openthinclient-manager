package org.openthinclient.console.configuration;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.services.Dhcp;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

@Configuration
@ComponentScan(basePackages="org.openthinclient.console")
public class HttpInvokerConfiguration {

    //
    // ------- Client side configuration -------
    //
    @Bean(name="packageManagerService")
    public FactoryBean<Object> packageManagerService() {
      final HttpInvokerProxyFactoryBean invokerProxyFactoryBean = new HttpInvokerProxyFactoryBean();
      invokerProxyFactoryBean.setServiceInterface(PackageManager.class);
      invokerProxyFactoryBean.setServiceUrl("http://localhost:8087/service/httpinvoker/package-manager");
      return invokerProxyFactoryBean;
    }
    
    @Bean(name="dhcpService")
    public FactoryBean<Object> dhcpService() {
      final HttpInvokerProxyFactoryBean invokerProxyFactoryBean = new HttpInvokerProxyFactoryBean();
      invokerProxyFactoryBean.setServiceInterface(Dhcp.class);
      invokerProxyFactoryBean.setServiceUrl("http://localhost:8087/service/httpinvoker/dhcp-service");
//      invokerProxyFactoryBean.setHttpInvokerRequestExecutor(httpInvokerRequestExecutor); HttpComponentsHttpInvokerRequestExecutor
      return invokerProxyFactoryBean;
    }
    
}

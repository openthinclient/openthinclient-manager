package org.openthinclient.console.configuration;

import org.openthinclient.pkgmgr.PackageManager;
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
      invokerProxyFactoryBean.setServiceUrl("http://localhost:8087/PackageManagerService");
      return invokerProxyFactoryBean;
    }
    
}

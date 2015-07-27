package org.openthinclient.manager.standalone.config.service;

import java.util.HashMap;

import org.openthinclient.pkgmgr.impl.PackageManagerImpl;
import org.openthinclient.service.dhcp.DhcpService;
import org.openthinclient.services.Dhcp;
import org.openthinclient.services.PackageManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.httpinvoker.SimpleHttpInvokerServiceExporter;
import org.springframework.remoting.support.SimpleHttpServerFactoryBean;

import com.sun.net.httpserver.HttpHandler;

@Configuration
public class HttpInvokerConfiguration {

	  //
	  // ------- Server side configuration for non ServletContainer environment -------
	  //
	  @Bean(name="SimpleHttpInvokerServiceExporter/PackageManagerImpl")
	  public SimpleHttpInvokerServiceExporter packageManagerServiceExporter(PackageManager packageManager) {
	    final SimpleHttpInvokerServiceExporter exporter = new SimpleHttpInvokerServiceExporter();
	    exporter.setServiceInterface(PackageManager.class);
	    exporter.setService(packageManager);
	    return exporter;
	  }

	  @Bean
	  public SimpleHttpServerFactoryBean httpServer(@Qualifier("SimpleHttpInvokerServiceExporter/PackageManagerImpl") SimpleHttpInvokerServiceExporter packageManagerServiceExporter, 
			  										@Qualifier("SimpleHttpInvokerServiceExporter/DhcpService") SimpleHttpInvokerServiceExporter dhcpServiceExporter) {
	    final SimpleHttpServerFactoryBean httpServer = new SimpleHttpServerFactoryBean();
	    final HashMap<String, HttpHandler> contexts = new HashMap<>();
	    contexts.put("/service/httpinvoker/package-manager", packageManagerServiceExporter);
	    contexts.put("/service/httpinvoker/dhcp-service", dhcpServiceExporter);
	    httpServer.setContexts(contexts);
	    httpServer.setPort(8087);
	    return httpServer;
	  }
	  
	  //
	  // ------- Server side configuration for non ServletContainer environment -------
	  //
	  @Bean(name="SimpleHttpInvokerServiceExporter/DhcpService")
	  public SimpleHttpInvokerServiceExporter dhcpServiceExporter(Dhcp dhcpService) {
	    final SimpleHttpInvokerServiceExporter exporter = new SimpleHttpInvokerServiceExporter();
	    exporter.setServiceInterface(Dhcp.class);
	    exporter.setService(dhcpService);
	    return exporter;
	  }

//	  @Bean
//	  public SimpleHttpServerFactoryBean httpServer(SimpleHttpInvokerServiceExporter dhcpServiceExporter) {
//	    final SimpleHttpServerFactoryBean httpServer = new SimpleHttpServerFactoryBean();
//	    final HashMap<String, HttpHandler> contexts = new HashMap<>();
//	    contexts.put("/service/httpinvoker/dhcp-service", dhcpServiceExporter);
//	    httpServer.setContexts(contexts);
//	    httpServer.setPort(8087);
//	    return httpServer;
//	  }
	  
	  
	  
}

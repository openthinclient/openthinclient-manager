package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.services.Dhcp;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;

@Configuration
public class HttpInvokerConfiguration {

	  //
	  // ------- Server side configuration for non ServletContainer environment -------
	  //
		@Bean(name = "/service/httpinvoker/package-manager")
		public HttpInvokerServiceExporter httpInvokerPackageManagerService(PackageManager packageManager) {
			final HttpInvokerServiceExporter serviceExporter = new HttpInvokerServiceExporter();
			serviceExporter.setService(packageManager);
			serviceExporter.setServiceInterface(PackageManager.class);
			return serviceExporter;
		}



	//
	  // ------- Server side configuration for non ServletContainer environment -------
	  //
	  @Bean(name="/service/httpinvoker/dhcp")
	  public HttpInvokerServiceExporter dhcpServiceExporter(Dhcp dhcpService) {
	    final HttpInvokerServiceExporter exporter = new HttpInvokerServiceExporter();
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.service.dhcp.DhcpService;
import org.openthinclient.service.dhcp.Remoted;
import org.openthinclient.service.dhcp.RemotedBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;


/**
 * DhcpServiceConfiguration
 * @author simon
 * @author joe
 */
@Configuration
public class DhcpServiceConfiguration {

	@Bean
	@DependsOn("apacheDsService")
	public DhcpService dhcpService(){
		return new DhcpService();
	}

	@Bean
	public Remoted remote(DhcpService dhcpService){
		return new RemotedBean(dhcpService);
	}

	@Bean(name = "/service/httpinvoker/dhcp-remoted-bean")
	public HttpInvokerServiceExporter httpInvokerDhcpService(Remoted remoted){
		final HttpInvokerServiceExporter serviceExporter = new HttpInvokerServiceExporter();
		serviceExporter.setService(remoted);
		serviceExporter.setServiceInterface(Remoted.class);
		return serviceExporter;
	}
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.common.model.service.UnrecognizedClientService;
import org.openthinclient.service.dhcp.DHCPService;
import org.openthinclient.service.dhcp.Remoted;
import org.openthinclient.service.dhcp.RemotedBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;


/**
 * DhcpServiceConfiguration
 *
 * @author simon
 * @author joe
 */
@Configuration
public class DhcpServiceConfiguration {

  @Bean
  @DependsOn("apacheDsService")
  public DHCPService dhcpService(RealmService realmService, ClientService clientService, UnrecognizedClientService unrecognizedClientService) throws Exception {
    return new DHCPService(realmService, clientService, unrecognizedClientService);
  }

  @Bean
  public Remoted remote(DHCPService dhcpService) {
    return new RemotedBean(dhcpService);
  }

  @Bean(name = "/service/httpinvoker/dhcp-remoted-bean")
  public HttpInvokerServiceExporter httpInvokerDhcpService(Remoted remoted) {
    final HttpInvokerServiceExporter serviceExporter = new HttpInvokerServiceExporter();
    serviceExporter.setService(remoted);
    serviceExporter.setServiceInterface(Remoted.class);
    return serviceExporter;
  }
}

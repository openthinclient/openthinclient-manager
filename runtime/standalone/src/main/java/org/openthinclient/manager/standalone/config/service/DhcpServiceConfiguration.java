/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.service.dhcp.DHCPService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class DhcpServiceConfiguration {

  @Bean
  @DependsOn("apacheDsService")
  public DHCPService dhcpService() throws Exception {
    return new DHCPService();
  }
}

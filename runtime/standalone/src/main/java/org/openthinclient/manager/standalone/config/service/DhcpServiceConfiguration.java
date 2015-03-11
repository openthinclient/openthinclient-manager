/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.service.dhcp.DhcpService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author simon
 */
@Configuration
public class DhcpServiceConfiguration {
	
	@Bean
	public DhcpService dhcpService(){
	
		return new DhcpService();
	}
}

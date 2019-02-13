package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.service.nfs.NFSService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NFSServiceConfiguration {

  @Bean
  public NFSService nfsService() {
    return new NFSService();
  }
}

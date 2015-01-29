package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.service.common.Service;
import org.openthinclient.service.apacheds.DirectoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApacheDSServiceConfiguration {

  @Bean
  public Service apacheDsService() {

    return new DirectoryService();

  }

}

package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.service.common.Service;
import org.openthinclient.service.apacheds.DirectoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;


@Configuration
public class ApacheDSServiceConfiguration {

  @Bean
  @Order(value = Ordered.HIGHEST_PRECEDENCE)
  public Service apacheDsService() {

    return new DirectoryService();

  }

}

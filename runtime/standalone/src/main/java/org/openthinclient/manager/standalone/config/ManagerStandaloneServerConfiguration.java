package org.openthinclient.manager.standalone.config;

import org.openthinclient.manager.standalone.service.ServiceBeanPostProcessor;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ManagerStandaloneServerConfiguration {

  /**
   * Creates the {@link org.openthinclient.manager.standalone.service.ServiceBeanPostProcessor} which controls the
   * lifecycle of {@link org.openthinclient.service.common.Service} instances.
   *
   * @return a {@link org.openthinclient.manager.standalone.service.ServiceBeanPostProcessor}
   * @param managerHome
   */
  @Bean
  public ServiceBeanPostProcessor serviceBeanPostProcessor(ManagerHome managerHome) {
    return new ServiceBeanPostProcessor(managerHome);
  }

  /**
   * Creates the {@link org.openthinclient.service.common.home.ManagerHome}
   */
  @Bean
  public ManagerHome managerHome() {
    final ManagerHomeFactory factory = new ManagerHomeFactory();
    return factory.create();
  }

}

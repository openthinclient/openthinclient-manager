package org.openthinclient.manager.standalone.config;

import org.openthinclient.db.conf.DataSourceConfiguration;
import org.openthinclient.manager.standalone.migrate.Migrations;
import org.openthinclient.manager.standalone.service.ServiceBeanPostProcessor;
import org.openthinclient.service.common.ServiceManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.web.WebApplicationConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ //
        WebApplicationConfiguration.class,//
        DataSourceConfiguration.class, //
        DirectoryServicesConfiguration.class //
})
public class ManagerStandaloneServerConfiguration {

  /**
   * Creates the {@link org.openthinclient.manager.standalone.service.ServiceBeanPostProcessor}
   * which controls the lifecycle of {@link org.openthinclient.service.common.Service} instances.
   *
   * @return a {@link org.openthinclient.manager.standalone.service.ServiceBeanPostProcessor}
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
    final ManagerHome managerHome = factory.create();

    Migrations.runEarlyMigrations(managerHome);

    return managerHome;
  }

  @Bean
  public ServiceManager serviceManager() {
    return new ServiceManager();
  }
}

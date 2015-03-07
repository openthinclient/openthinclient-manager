package org.openthinclient.manager.standalone;

import org.openthinclient.service.common.Service;
import org.openthinclient.manager.standalone.config.ManagerStandaloneServerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class ManagerStandaloneServerApplication {

  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(ManagerStandaloneServerConfiguration.class, args);

    context.start();

//    context.getBeansOfType(Service.class).forEach((name, service) -> {
//      System.err.println(service);
//    });
  }

}

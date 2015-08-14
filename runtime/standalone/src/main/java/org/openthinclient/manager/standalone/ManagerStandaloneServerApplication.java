package org.openthinclient.manager.standalone;

import org.openthinclient.manager.standalone.config.ManagerStandaloneServerConfiguration;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.WizardApplicationConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class ManagerStandaloneServerApplication {

  public static void main(String[] args) {


    final ManagerHomeFactory managerHomeFactory = new ManagerHomeFactory();

    ConfigurableApplicationContext context;
    if (managerHomeFactory.isManagerHomeValidAndInstalled()) {
      // start the default manager application
      context = SpringApplication.run(ManagerStandaloneServerConfiguration.class, args);
    } else {
      // execute the first time installation
      context = SpringApplication.run(WizardApplicationConfiguration.class, args);
    }
    context.start();

  }

}

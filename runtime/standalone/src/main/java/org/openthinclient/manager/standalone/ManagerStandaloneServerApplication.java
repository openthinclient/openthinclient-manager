package org.openthinclient.manager.standalone;

import org.openthinclient.manager.standalone.config.ManagerStandaloneServerConfiguration;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.WizardApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class ManagerStandaloneServerApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ManagerStandaloneServerApplication.class);

  public static void main(String[] args) {


    final ManagerHomeFactory managerHomeFactory = new ManagerHomeFactory();

    ConfigurableApplicationContext context;
    if (managerHomeFactory.isManagerHomeValidAndInstalled()) {

      LOG.info("\n" + //
                      "========================================================\n" + //
                      "\n" + //
                      "Starting the open thinclient server\n" + //
                      "\n" + //
                      "Home Directory:\n" + //
                      "{}\n" + //
                      "========================================================", //
              managerHomeFactory.getManagerHomeDirectory());

      // start the default manager application
      context = SpringApplication.run(ManagerStandaloneServerConfiguration.class, args);
    } else {
      LOG.info("\n" + //
                      "========================================================\n" + //
                      "\n" + //
                      "open thinclient\n" + //
                      "First Time Installation\n" + //
                      "\n" + //
                      "========================================================", //
              managerHomeFactory.getManagerHomeDirectory());
      // execute the first time installation
      context = SpringApplication.run(WizardApplicationConfiguration.class, args);
    }
    context.start();

  }

}

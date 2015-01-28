package org.openthinclient.manager.standalone;

import org.openthinclient.manager.standalone.config.ManagerStandaloneServerConfiguration;
import org.springframework.boot.SpringApplication;

public class ManagerStandaloneServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ManagerStandaloneServerConfiguration.class, args);
  }

}

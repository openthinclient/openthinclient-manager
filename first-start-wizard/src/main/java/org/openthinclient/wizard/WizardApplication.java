package org.openthinclient.wizard;

import org.openthinclient.wizard.conf.WizardApplicationConfiguration;
import org.springframework.boot.SpringApplication;

public class WizardApplication {

  public static void main(String[] args) {
    SpringApplication.run(WizardApplicationConfiguration.class, args);
  }

}

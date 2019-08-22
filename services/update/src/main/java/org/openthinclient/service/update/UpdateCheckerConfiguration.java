package org.openthinclient.service.update;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UpdateCheckerConfiguration {

  @Bean
  public UpdateChecker updateChecker() {
    return new UpdateChecker();
  }

}

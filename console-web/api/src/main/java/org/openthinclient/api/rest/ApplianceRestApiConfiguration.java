package org.openthinclient.api.rest;

import org.openthinclient.api.rest.appliance.ApplianceResource;
import org.openthinclient.api.rest.appliance.TokenManager;
import org.openthinclient.service.common.home.impl.ApplianceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplianceRestApiConfiguration {

  @Autowired
  public ApplianceConfiguration applianceConfiguration;

  @Bean
  // FIXME only enable if the appliance mode has been enabled.
  public ApplianceResource applianceResource() {
    return new ApplianceResource(tokenManager());
  }

  @Bean
  public TokenManager tokenManager() {
    return new TokenManager(applianceConfiguration);
  }

}

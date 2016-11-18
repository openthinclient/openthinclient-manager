package org.openthinclient.manager.standalone.config;

import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.schema.provider.ServerLocalSchemaProvider;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DefaultLDAPClientService;
import org.openthinclient.common.model.service.DefaultLDAPRealmService;
import org.openthinclient.common.model.service.DefaultLDAPUnrecognizedClientService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.common.model.service.UnrecognizedClientService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DirectoryServicesConfiguration {

  @Bean
  public RealmService realmService() {
    return new DefaultLDAPRealmService(schemaProvider());
  }

  @Bean
  public SchemaProvider schemaProvider() {
    return new ServerLocalSchemaProvider();
  }

  @Bean
  public ClientService clientService() {
    return new DefaultLDAPClientService(realmService());
  }

  @Bean
  public UnrecognizedClientService unrecognizedClientService() {
    return new DefaultLDAPUnrecognizedClientService(realmService());
  }
}

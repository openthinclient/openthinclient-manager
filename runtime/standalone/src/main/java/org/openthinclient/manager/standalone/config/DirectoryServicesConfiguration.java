package org.openthinclient.manager.standalone.config;

import org.openthinclient.common.model.schema.provider.AbstractSchemaProvider;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.schema.provider.ServerLocalSchemaProvider;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DefaultLDAPApplicationService;
import org.openthinclient.common.model.service.DefaultLDAPClientService;
import org.openthinclient.common.model.service.DefaultLDAPRealmService;
import org.openthinclient.common.model.service.DefaultLDAPUnrecognizedClientService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.common.model.service.UnrecognizedClientService;
import org.openthinclient.service.common.home.ManagerHome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class DirectoryServicesConfiguration {

  @Autowired
  ManagerHome managerHome;

  @Bean
  public RealmService realmService() {
    return new DefaultLDAPRealmService(schemaProvider());
  }

  @Bean
  public SchemaProvider schemaProvider() {
    final File homeDirectory = managerHome.getLocation();

    // FIXME this is also implemented in PXEConfigTFTPExport, as it can not yet reach the spring context
    return new ServerLocalSchemaProvider(
            homeDirectory.toPath().resolve("nfs").resolve("root").resolve(AbstractSchemaProvider.SCHEMA_PATH)
    );

  }

  @Bean
  public ClientService clientService() {
    return new DefaultLDAPClientService(realmService());
  }

  @Bean
  public UnrecognizedClientService unrecognizedClientService() {
    return new DefaultLDAPUnrecognizedClientService(realmService());
  }

  @Bean
  public ApplicationService applicationService() {
    return new DefaultLDAPApplicationService(realmService());
  }
}

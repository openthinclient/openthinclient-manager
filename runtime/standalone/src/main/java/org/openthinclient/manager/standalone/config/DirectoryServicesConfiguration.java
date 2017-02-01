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
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
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
  public LDAPConnectionDescriptor ldapConnectionDescriptor() {
    LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
    lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
    lcd.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);

    final DirectoryServiceConfiguration configuration = managerHome.getConfiguration(DirectoryServiceConfiguration.class);

    lcd.setCallbackHandler(new UsernamePasswordHandler(configuration.getContextSecurityPrincipal(),
            configuration.getContextSecurityCredentials().toCharArray()));

    return lcd;
  }

  @Bean
  public RealmService realmService() {
    return new DefaultLDAPRealmService(schemaProvider(), ldapConnectionDescriptor());
  }

  @Bean
  public SchemaProvider schemaProvider() {
    final File homeDirectory = managerHome.getLocation();

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

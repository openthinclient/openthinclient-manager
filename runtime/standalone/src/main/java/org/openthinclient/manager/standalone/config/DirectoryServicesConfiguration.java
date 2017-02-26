package org.openthinclient.manager.standalone.config;

import org.openthinclient.common.config.LDAPServicesConfiguration;
import org.openthinclient.common.model.schema.provider.AbstractSchemaProvider;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.schema.provider.ServerLocalSchemaProvider;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.File;

@Configuration
@Import(LDAPServicesConfiguration.class)
public class DirectoryServicesConfiguration {
  // FIXME these contents are essentially the same as in org.openthinclient.wizard.install.BootstrapLDAPInstallStep.BootstrapConfiguration.
  // due to the current project layout, duplicating this is the only viable option at this point in time

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
  public SchemaProvider schemaProvider() {
    final File homeDirectory = managerHome.getLocation();

    return new ServerLocalSchemaProvider(
            homeDirectory.toPath().resolve("nfs").resolve("root").resolve(AbstractSchemaProvider.SCHEMA_PATH)
    );

  }

}

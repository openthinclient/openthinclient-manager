package org.openthinclient.common.config;

import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LDAPServicesConfiguration {

  @Autowired
  LDAPConnectionDescriptor ldapConnectionDescriptor;

  @Autowired
  SchemaProvider schemaProvider;

  @Bean
  public RealmService realmService() {
    return new DefaultLDAPRealmService(schemaProvider, ldapConnectionDescriptor);
  }

  @Bean
  public ClientService clientService() {
    return new DefaultLDAPClientService(realmService());
  }

  @Bean
  public UserService userService() {
    return new DefaultLDAPUserService(realmService());
  }

  @Bean
  public UnrecognizedClientService unrecognizedClientService() {
    return new DefaultLDAPUnrecognizedClientService(realmService());
  }

  @Bean
  public ApplicationService applicationService() {
    return new DefaultLDAPApplicationService(realmService());
  }

  @Bean
  public DeviceService deviceService() {
    return new DefaultLDAPDeviceService(realmService());
  }

  @Bean
  public HardwareTypeService hardwareTypeService() {
    return new DefaultLDAPHardwareTypeService(realmService());
  }

  @Bean
  public LocationService locationService() {
    return new DefaultLDAPLocationService(realmService());
  }

  @Bean
  public PrinterService printerService() {
    return new DefaultLDAPPrinterService(realmService());
  }

}

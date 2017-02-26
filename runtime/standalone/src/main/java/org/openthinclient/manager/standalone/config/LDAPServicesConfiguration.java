package org.openthinclient.manager.standalone.config;

import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DefaultLDAPApplicationService;
import org.openthinclient.common.model.service.DefaultLDAPClientService;
import org.openthinclient.common.model.service.DefaultLDAPDeviceService;
import org.openthinclient.common.model.service.DefaultLDAPHardwareTypeService;
import org.openthinclient.common.model.service.DefaultLDAPLocationService;
import org.openthinclient.common.model.service.DefaultLDAPPrinterService;
import org.openthinclient.common.model.service.DefaultLDAPRealmService;
import org.openthinclient.common.model.service.DefaultLDAPUnrecognizedClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.common.model.service.UnrecognizedClientService;
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

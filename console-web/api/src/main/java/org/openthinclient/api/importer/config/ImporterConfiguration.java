package org.openthinclient.api.importer.config;

import org.mapstruct.factory.Mappers;
import org.openthinclient.api.importer.impl.ImportModelMapper;
import org.openthinclient.api.importer.impl.ProfileReferenceCreator;
import org.openthinclient.api.importer.impl.ProfileReferenceResolver;
import org.openthinclient.api.importer.impl.ProfileSchemaConfigurer;
import org.openthinclient.api.importer.impl.RestModelImporter;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImporterConfiguration {

  @Autowired
  private HardwareTypeService hardwareTypeService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private DeviceService deviceService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private PrinterService printerService;

  @Bean
  public ProfileReferenceResolver profileResolver() {
    return new ProfileReferenceResolver(hardwareTypeService, applicationService, clientService, deviceService, locationService, printerService);
  }
  @Bean
  public ProfileReferenceCreator profileReferenceCreator() {
    return new ProfileReferenceCreator();
  }

  @Bean
  public ProfileSchemaConfigurer profileSchemaConfigurer() {
    return new ProfileSchemaConfigurer();
  }

  @Bean
  public ImportModelMapper importModelMapper() {
    return Mappers.getMapper(ImportModelMapper.class);
  }

  @Bean
  public RestModelImporter importer() {
    return new RestModelImporter(importModelMapper(), hardwareTypeService, applicationService, clientService, deviceService, locationService, printerService);
  }

}

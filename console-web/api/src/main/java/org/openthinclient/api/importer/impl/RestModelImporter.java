package org.openthinclient.api.importer.impl;

import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;

public class RestModelImporter {

  private final ImportModelMapper mapper;
  private final HardwareTypeService hardwareTypeService;
  private final ApplicationService applicationService;
  private final ClientService clientService;
  private final DeviceService deviceService;
  private final LocationService locationService;
  private final PrinterService printerService;

  public RestModelImporter(ImportModelMapper mapper, HardwareTypeService hardwareTypeService, ApplicationService applicationService, ClientService clientService, DeviceService deviceService, LocationService locationService, PrinterService printerService) {
    this.mapper = mapper;
    this.hardwareTypeService = hardwareTypeService;
    this.applicationService = applicationService;
    this.clientService = clientService;
    this.deviceService = deviceService;
    this.locationService = locationService;
    this.printerService = printerService;
  }

  //  public Client importClient(org.openthinclient.api.rest.model.Client restModel) {
//
//
//
//  }

  public HardwareType importHardwareType(ImportableHardwareType importable) {

    final HardwareType hardwareType = mapper.fromImportable(importable);

    hardwareTypeService.save(hardwareType);

    return hardwareType;
  }

}

package org.openthinclient.api.importer.impl;

import org.openthinclient.api.importer.model.ImportableClient;
import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.api.importer.model.ImportableLocation;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Printer;
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

  public Device importDevice(org.openthinclient.api.rest.model.Device importable) {
    final Device device = mapper.fromImportable(importable);

    deviceService.save(device);

    return device;
  }

  public HardwareType importHardwareType(ImportableHardwareType importable) {

    final HardwareType hardwareType = mapper.fromImportable(importable);

    hardwareTypeService.save(hardwareType);

    return hardwareType;
  }

  public Printer importPrinter(org.openthinclient.api.rest.model.Printer importable) {
    final Printer printer = mapper.fromImportable(importable);

    printerService.save(printer);

    return printer;
  }

  public Location importLocation(ImportableLocation importable) {

    final Location location = mapper.fromImportable(importable);

    locationService.save(location);

    return location;
  }

  public Application importApplication(org.openthinclient.api.rest.model.Application importable) {

    final Application application = mapper.fromImportable(importable);

    applicationService.save(application);

    return application;
  }

  public Client importClient(ImportableClient importable) {

    final Client client = mapper.fromImportable(importable);

    clientService.save(client);

    return client;
  }

}

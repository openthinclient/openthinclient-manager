package org.openthinclient.api.importer.impl;

import com.google.common.base.Objects;

import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.api.importer.model.ProfileReference;
import org.openthinclient.api.importer.model.ProfileType;
import org.openthinclient.api.rest.model.Configuration;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;
import org.openthinclient.common.model.service.RealmService;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class RestModelImporter {

  private final RealmService realmService;
  private final HardwareTypeService hardwareTypeService;
  private final ApplicationService applicationService;
  private final ClientService clientService;
  private final DeviceService deviceService;
  private final LocationService locationService;
  private final PrinterService printerService;

  public RestModelImporter(RealmService realmService, HardwareTypeService hardwareTypeService, ApplicationService applicationService, ClientService clientService, DeviceService deviceService, LocationService locationService, PrinterService printerService) {
    this.realmService = realmService;
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

    final HardwareType hardwareType = new HardwareType();
    // FIXME validate name!
    hardwareType.setName(importable.getName());

    hardwareType.setDevices(findReferenced(ProfileType.DEVICE, importable.getDevices()));
    hardwareType.setHardwareTypes(findReferenced(ProfileType.HARDWARE_TYPE, importable.getHardwareTypes()));

    // FIXME determine the required schema
    // apply configuration
    final Configuration configuration = importable.getConfiguration();

    applyConfiguration(hardwareType, configuration);

    hardwareTypeService.save(hardwareType);


    return hardwareType;

  }

  private void applyConfiguration(HardwareType hardwareType, Configuration configuration) {
    for (String key : configuration.getAdditionalProperties().keySet()) {
      // FIXME validate that the key and value actually match the associated schema!
      hardwareType.setValue(key, "" + configuration.getAdditionalProperties().get(key));
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends Profile> Set<T> findReferenced(ProfileType type, Set<ProfileReference> refs) {

    validateReferences(type, refs);

    switch (type) {
      case APPLICATION:
        return (Set<T>) resolveReferences(refs, applicationService::findByName);
      case HARDWARE_TYPE:
        return (Set<T>) resolveReferences(refs, hardwareTypeService::findByName);
      case DEVICE:
        return (Set<T>) resolveReferences(refs, deviceService::findByName);
      case LOCATION:
        return (Set<T>) resolveReferences(refs, locationService::findByName);
      case CLIENT:
        return (Set<T>) resolveReferences(refs, clientService::findByName);
      case PRINTER:
        return (Set<T>) resolveReferences(refs, printerService::findByName);
    }
    throw new IllegalArgumentException("Failed to process reference. Unknown profile type: " + type);
  }

  private <T extends Profile> HashSet<T> resolveReferences(Set<ProfileReference> refs, Function<String, T> lookup) {
    final HashSet<T> result = new HashSet<>();
    for (ProfileReference ref : refs) {

      final T target = lookup.apply(ref.getName());

      if (target == null)
        throw new MissingReferencedObjectException(ref);

      result.add(target);
    }
    return result;
  }

  private void validateReferences(ProfileType type, Set<ProfileReference> refs) {
    for (ProfileReference ref : refs) {

      if (!Objects.equal(type, ref.getType())) {
        throw new ImportException("Illegal reference: '" + ref.getCompactRepresentation() +
                "' refers to type " + ref.getType() + " but must refer to " + type);
      }
    }
  }


}

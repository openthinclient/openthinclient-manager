package org.openthinclient.api.importer.impl;

import com.google.common.base.Objects;

import org.mapstruct.TargetType;
import org.openthinclient.api.importer.model.ProfileReference;
import org.openthinclient.api.importer.model.ProfileType;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Function;

public class ProfileReferenceResolver {

  private final HardwareTypeService hardwareTypeService;
  private final ApplicationService applicationService;
  private final ClientService clientService;
  private final DeviceService deviceService;
  private final LocationService locationService;
  private final PrinterService printerService;

  @Autowired
  public ProfileReferenceResolver(HardwareTypeService hardwareTypeService, ApplicationService applicationService, ClientService clientService, DeviceService deviceService, LocationService locationService, PrinterService printerService) {
    this.hardwareTypeService = hardwareTypeService;
    this.applicationService = applicationService;
    this.clientService = clientService;
    this.deviceService = deviceService;
    this.locationService = locationService;
    this.printerService = printerService;
  }

  public <T extends Profile> T resolve(ProfileReference reference, @TargetType Class<T> profileType) {

    ProfileType expected = null;
    for (ProfileType cur : ProfileType.values()) {
      if (Objects.equal(cur.getTargetType(), profileType)) {
        expected = cur;
        break;
      }
    }
    if (expected == null)
      throw new IllegalArgumentException("Unsupported type of profile: " + profileType);

    validateReference(expected, reference);

    switch (expected) {
      case APPLICATION:
        return (T) resolveReferences(reference, applicationService::findByName);
      case HARDWARETYPE:
        return (T) resolveReferences(reference, hardwareTypeService::findByName);
      case DEVICE:
        return (T) resolveReferences(reference, deviceService::findByName);
      case LOCATION:
        return (T) resolveReferences(reference, locationService::findByName);
      case CLIENT:
        return (T) resolveReferences(reference, clientService::findByName);
      case PRINTER:
        return (T) resolveReferences(reference, printerService::findByName);
    }
    throw new IllegalArgumentException("Failed to process reference. Unknown profile type: " + profileType);

  }


  private <T extends Profile> T resolveReferences(ProfileReference ref, Function<String, T> lookup) {

    final T target = lookup.apply(ref.getName());

    if (target == null)
      throw new MissingReferencedObjectException(ref);

    return target;
  }

  private void validateReference(ProfileType type, ProfileReference ref) {
    if (!Objects.equal(type, ref.getType())) {
      throw new ImportException("Illegal reference: '" + ref.getCompactRepresentation() +
              "' refers to type " + ref.getType() + " but must refer to " + type);
    }
  }

}

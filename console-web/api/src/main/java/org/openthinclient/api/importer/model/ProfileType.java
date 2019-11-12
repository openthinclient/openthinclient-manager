package org.openthinclient.api.importer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.ClientGroup;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Printer;

public enum ProfileType {

  APPLICATION(Application.class),
  HARDWARETYPE(HardwareType.class),
  DEVICE(Device.class),
  LOCATION(Location.class),
  CLIENT(Client.class),
  PRINTER(Printer.class),
  APPLICATION_GROUP(ApplicationGroup.class),
  CLIENT_GROUP(ClientGroup.class);

  private final Class<? extends DirectoryObject> targetType;

  ProfileType(Class<? extends DirectoryObject> targetType) {
    this.targetType = targetType;
  }

  @JsonCreator
  public static ProfileType create(String raw) {
    return valueOf(raw.toUpperCase());
  }

  public Class<? extends DirectoryObject> getTargetType() {
    return targetType;
  }

  @JsonValue
  public String asJsonValue() {
    return name().toLowerCase();
  }
}

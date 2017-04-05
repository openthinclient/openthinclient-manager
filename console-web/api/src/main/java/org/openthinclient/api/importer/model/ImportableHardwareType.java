package org.openthinclient.api.importer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openthinclient.api.rest.model.HardwareType;

import java.util.HashSet;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportableHardwareType extends HardwareType {

  @JsonProperty
  private Set<ProfileReference> devices;

  @JsonProperty
  private Set<ProfileReference> hardwareTypes;

  public Set<ProfileReference> getDevices() {
    if (devices == null)
      devices = new HashSet<>();
    return devices;
  }

  public Set<ProfileReference> getHardwareTypes() {
    if (hardwareTypes == null)
      hardwareTypes = new HashSet<>();
    return hardwareTypes;
  }
}

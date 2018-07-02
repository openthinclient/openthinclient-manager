package org.openthinclient.api.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openthinclient.api.importer.model.ProfileReference;
import org.openthinclient.api.importer.model.ProfileType;

import java.util.HashSet;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HardwareType extends AbstractProfileObject {

  @JsonProperty
  private Set<ProfileReference> devices = new HashSet<>();

  public HardwareType() {
    super(ProfileType.HARDWARETYPE);
  }

  public Set<ProfileReference> getDevices() {
    return devices;
  }

  public void setDevices(Set<ProfileReference> devices) {
    this.devices = devices;
  }
}

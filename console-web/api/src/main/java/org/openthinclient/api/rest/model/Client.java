package org.openthinclient.api.rest.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openthinclient.api.importer.model.ProfileType;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Client extends AbstractProfileObject {

  @JsonIgnore
  private final Map<String, Object> additionalProperties = new HashMap<String, Object>();
  @JsonProperty("macAddress")
  @NotNull
  private String macAddress;
  @JsonProperty
  private Location location;
  @JsonProperty
  private HardwareType hardwareType;

  public Client() {
    super(ProfileType.CLIENT);
  }

  public HardwareType getHardwareType() {
    return hardwareType;
  }

  public void setHardwareType(HardwareType hardwareType) {
    this.hardwareType = hardwareType;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public String getMacAddress() {
    return macAddress;
  }

  public void setMacAddress(String macAddress) {
    this.macAddress = macAddress;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

}

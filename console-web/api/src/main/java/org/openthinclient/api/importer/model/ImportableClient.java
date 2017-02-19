package org.openthinclient.api.importer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openthinclient.api.rest.model.AbstractProfileObject;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportableClient extends AbstractProfileObject {

  @JsonProperty
  private Set<ProfileReference> applications;
  @JsonProperty
  private Set<ProfileReference> printers;
  @JsonProperty
  private Set<ProfileReference> devices;
  @JsonProperty(required = true)
  private ProfileReference hardwareType;

  // keep this in sync with org.openthinclient.api.rest.model.Client.macAddress.
  @JsonProperty("macAddress")
  @NotNull
  private String macAddress;


  public ImportableClient() {
    super(ProfileType.CLIENT);
  }

  public Set<ProfileReference> getApplications() {
    if (applications == null) {
      applications = new HashSet<>();
    }
    return applications;
  }

  public Set<ProfileReference> getPrinters() {
    if (printers == null)
      printers = new HashSet<>();
    return printers;
  }

  public Set<ProfileReference> getDevices() {
    if (devices == null)
      devices = new HashSet<>();
    return devices;
  }

  public ProfileReference getHardwareType() {
    return hardwareType;
  }

  public void setHardwareType(ProfileReference hardwareType) {
    this.hardwareType = hardwareType;
  }

  public String getMacAddress() {

    return macAddress;
  }

  public void setMacAddress(String macAddress) {
    this.macAddress = macAddress;
  }
}

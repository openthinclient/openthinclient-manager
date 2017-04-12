package org.openthinclient.api.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.openthinclient.api.importer.model.ProfileType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HardwareType extends AbstractProfileObject {
  public HardwareType() {
    super(ProfileType.HARDWARETYPE);
  }
}

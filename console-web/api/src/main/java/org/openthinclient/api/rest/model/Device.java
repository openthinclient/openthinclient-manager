package org.openthinclient.api.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.openthinclient.api.importer.model.ProfileType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Device extends AbstractProfileObject {

  public Device() {
    super(ProfileType.DEVICE);
  }
}

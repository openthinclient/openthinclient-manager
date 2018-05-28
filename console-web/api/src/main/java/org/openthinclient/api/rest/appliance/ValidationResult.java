package org.openthinclient.api.rest.appliance;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidationResult {

  @JsonProperty
  private boolean valid;

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }
}

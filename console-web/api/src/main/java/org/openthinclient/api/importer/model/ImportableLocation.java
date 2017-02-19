package org.openthinclient.api.importer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openthinclient.api.rest.model.Location;

import java.util.HashSet;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportableLocation extends Location {

  @JsonProperty
  private Set<ProfileReference> printers;

  public Set<ProfileReference> getPrinters() {
    if (printers == null)
      printers = new HashSet<>();
    return printers;
  }

}

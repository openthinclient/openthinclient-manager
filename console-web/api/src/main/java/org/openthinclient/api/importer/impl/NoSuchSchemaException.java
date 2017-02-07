package org.openthinclient.api.importer.impl;

import org.openthinclient.common.model.Profile;

public class NoSuchSchemaException extends ImportException {
  public NoSuchSchemaException(Class<? extends Profile> profileType, String subtype) {
    super("No matching schema could be found for " + profileType + " and schema name " + subtype);
  }
}

package org.openthinclient.api.importer.impl;

import org.openthinclient.common.model.DirectoryObject;

public class NoSuchSchemaException extends ImportException {
  public NoSuchSchemaException(Class<? extends DirectoryObject> profileType, String subtype) {
    super("No matching schema could be found for " + profileType + " and schema name " + subtype);
  }
}

package org.openthinclient.api.importer.impl;

import org.openthinclient.api.importer.model.ProfileReference;

/**
 * An exception indicating that a {@link org.openthinclient.api.importer.model.ProfileReference}
 * could not be resolved.
 */
public class MissingReferencedObjectException extends ImportException {

  private final ProfileReference reference;

  public MissingReferencedObjectException(ProfileReference reference) {
    super("Referenced object of type " + reference.getType() + " with name '" + reference.getName() +
            "' could not be found");
    this.reference = reference;
  }
}

package org.openthinclient.api.importer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import org.openthinclient.common.model.Client;

/**
 * An object used to express the relationship of one model element to another. An example is the
 * relation of {@link Client#getApplications() the client to the associated applications}.
 */
public class ProfileReference {

  @JsonCreator
  public static ProfileReference parse(String raw) {

    final int idx = raw.indexOf(':');
    if (idx != -1) {

      // to make writing the type more convenient, dashes will be converted to underscores.
      final String type = raw.substring(0, idx).replace('-', '_');
      return new ProfileReference(ProfileType.valueOf( //
              type.toUpperCase()), //
              raw.substring(idx + 1, raw.length()) //
      );

    }

    throw new IllegalArgumentException("Unexpected format. Required format: [TYPE]:[NAME]");

  }

  /**
   * The type of the target profile that shall be associated with the referencing profile.
   */
  private ProfileType type;

  private String name;

  /**
   * Used for serialization purposes only.
   */
  @Deprecated
  public ProfileReference() {
  }

  public ProfileReference(ProfileType type, String name) {
    this.type = type;
    this.name = name;
  }

  public ProfileType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  @JsonValue
  public String getCompactRepresentation() {

    return type.name().replace('_','-').toLowerCase() + ":" + this.name;

  }
}

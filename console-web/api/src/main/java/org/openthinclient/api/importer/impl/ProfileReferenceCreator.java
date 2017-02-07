package org.openthinclient.api.importer.impl;

import org.openthinclient.api.importer.model.ProfileReference;
import org.openthinclient.api.importer.model.ProfileType;
import org.openthinclient.common.model.Profile;

public class ProfileReferenceCreator {


  public ProfileReference create(Profile profile) {

    for (ProfileType type : ProfileType.values()) {

      if (profile.getClass().equals(type.getTargetType())) {

        return new ProfileReference(type, profile.getName());

      }

    }

    throw new IllegalArgumentException("Unsupported type of profile: " + profile);

  }
}

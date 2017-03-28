package org.openthinclient.api.importer.impl;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.openthinclient.api.importer.model.ImportableClient;
import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.api.importer.model.ImportableLocation;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Profile;

@Mapper(uses = {ProfileReferenceResolver.class, ProfileReferenceCreator.class, ProfileSchemaConfigurer.class,
                ApplicationProfileMembersMapper.class}, componentModel = "spring")
public interface ImportModelMapper {

  ImportableHardwareType toImportable(HardwareType hw);

  HardwareType fromImportable(ImportableHardwareType hw);

  Device fromImportable(org.openthinclient.api.rest.model.Device device);

  Printer fromImportable(org.openthinclient.api.rest.model.Printer printer);

  Location fromImportable(ImportableLocation location);

  Application fromImportable(org.openthinclient.api.rest.model.Application application);

  Client fromImportable(ImportableClient importableClient);

  @AfterMapping
  default void applyConfiguration(Profile source, @MappingTarget AbstractProfileObject profileObject) {

    source.getProperties().getMap().forEach((k,v) -> {
      profileObject.getConfiguration().setAdditionalProperty(k, v);
    });

    // the subtype describes the schema that shall be used. The name of the schema is stored in the description of the properties
    profileObject.setSubtype(source.getProperties().getDescription());

  }

}

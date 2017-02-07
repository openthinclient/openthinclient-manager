package org.openthinclient.api.importer.impl;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Profile;

@Mapper(uses = {ProfileReferenceResolver.class, ProfileReferenceCreator.class, ProfileSchemaConfigurer.class}, componentModel = "spring")
public interface ImportModelMapper {

  ImportableHardwareType toImportable(HardwareType hw);

  HardwareType fromImportable(ImportableHardwareType hw);

  @AfterMapping
  default void applyConfiguration(Profile source, @MappingTarget AbstractProfileObject profileObject) {

    source.getProperties().getMap().forEach((k,v) -> {
      profileObject.getConfiguration().setAdditionalProperty(k, v);
    });

    // the subtype describes the schema that shall be used. The name of the schema is stored in the description of the properties
    profileObject.setSubtype(source.getProperties().getDescription());

  }

}

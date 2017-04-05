package org.openthinclient.api.importer.impl;

import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Resolves the {@link org.openthinclient.common.model.schema.Schema} for a given {@link
 * org.openthinclient.api.rest.model.AbstractProfileObject} and applies the appropriate
 * configuration settings.
 */
public class ProfileSchemaConfigurer {

  @Autowired
  private SchemaProvider schemaProvider;

  @AfterMapping
  public void applyConfiguration(AbstractProfileObject source, @MappingTarget Profile profile) {

    // resolve the schema using the target type

    final Schema schema = schemaProvider.getSchema(source.getType().getTargetType(), source.getSubtype());

    if (schema == null)
      throw new NoSuchSchemaException(source.getType().getTargetType(), source.getSubtype());

    profile.setSchema(schema);

    source.getConfiguration().getAdditionalProperties().forEach((k, v) -> {
      // FIXME validate that the key and value actually match the associated schema!
      profile.setValue(k, "" + v);
    });

  }


}

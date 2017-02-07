package org.openthinclient.api.importer.impl;

import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.AbstractSchemaProvider;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClasspathSchemaProvider extends AbstractSchemaProvider {
  private static final String[] APPLICATION_SCHEMAS = {"browser", "rdesktop"};
  private static final String[] DEVICE_SCHEMAS = {"autologin", "display", "keyboard", "nfs"};


  @Override
  protected List<Schema> loadDefaultSchema(String profileTypeName) throws SchemaLoadingException {

    final InputStream is = getClass().getResourceAsStream("/test-schema/" + profileTypeName + ".xml");

    if (is == null)
      return Collections.emptyList();

    return Collections.singletonList(loadSchema(is));

  }

  @Override
  protected List<Schema> loadAllSchemas(String profileTypeName) throws SchemaLoadingException {

    String[] schemas;
    if (profileTypeName.equals("application")) {
      schemas = APPLICATION_SCHEMAS;
    } else if (profileTypeName.equals("device")) {
      schemas = DEVICE_SCHEMAS;
    } else {
      return Collections.emptyList();
    }

    final List<Schema> result = new ArrayList<>();

    for (String schema : schemas) {

      final InputStream is = getClass().getResourceAsStream("/test-schema/" + profileTypeName + "/" + schema + ".xml");

      if (is == null)
        throw new SchemaLoadingException("No such schema: " + profileTypeName + "/" + schema);

      result.add(loadSchema(is));

    }

    return result;

  }
}

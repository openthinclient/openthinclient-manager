package org.openthinclient.api.importer.impl;

import org.junit.Test;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Client;

import static org.junit.Assert.assertNotNull;

public class ClasspathSchemaProviderTest {
  @Test
  public void testBaseSchemasAccessible() throws Exception {

    final ClasspathSchemaProvider schemaProvider = new ClasspathSchemaProvider();

    assertNotNull(schemaProvider.getSchema(Client.class, null));
    assertNotNull(schemaProvider.getSchema(Client.class, "client"));

  }

  @Test
  public void testSpecificSchemasAccessible() throws Exception {
    final ClasspathSchemaProvider schemaProvider = new ClasspathSchemaProvider();

    assertNotNull(schemaProvider.getSchema(Application.class, "browser"));
    assertNotNull(schemaProvider.getSchema(Application.class, "rdesktop"));

  }
}
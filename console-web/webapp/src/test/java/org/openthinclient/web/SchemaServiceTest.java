package org.openthinclient.web;

import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchemaServiceTest {

  @Test
  public void testIsSchemaFilePath() throws Exception {

    final SchemaService service = new SchemaService(null, null, null, null);


    assertTrue(service.isSchemaFilePath(Paths.get("schema", "application", "rdesktop.xml")));
    assertFalse(service.isSchemaFilePath(Paths.get("schema", "application.xml")));

    assertFalse(service.isSchemaFilePath(Paths.get("schema", "somedir", "application.yml")));


  }
}
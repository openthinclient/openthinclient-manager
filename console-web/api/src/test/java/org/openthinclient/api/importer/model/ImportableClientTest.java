package org.openthinclient.api.importer.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class ImportableClientTest {

  private static final String EXPECTED = "{\n" +
          "  \"name\" : \"My Smart Client\",\n" +
          "  \"type\" : \"client\",\n" +
          "  \"configuration\" : {\n" +
          "    \"app.some-setting\" : \"false\",\n" +
          "    \"app.some-other-setting\" : \"yeay\"\n" +
          "  },\n" +
          "  \"applications\" : [ \"application:Firefox\", \"application:Command Line\" ],\n" +
          "  \"printers\" : [ ],\n" +
          "  \"devices\" : [ ],\n" +
          "  \"hardwareType\" : \"hardwaretype:My Hardware Type\"\n" +
          "}";

  @Test
  public void testSerializeImportableClient() throws Exception {

    final ImportableClient client = new ImportableClient();
    client.setName("My Smart Client");

    client.setHardwareType(ProfileReference.parse("hardwaretype:My Hardware Type"));

    client.getApplications().add(ProfileReference.parse("application:Firefox"));
    client.getApplications().add(ProfileReference.parse("application:Command Line"));

    client.getConfiguration().setAdditionalProperty("app.some-setting", "false");
    client.getConfiguration().setAdditionalProperty("app.some-other-setting", "yeay");

    final ObjectMapper mapper = new ObjectMapper();


    final StringWriter sw = new StringWriter();
    mapper.writer().withDefaultPrettyPrinter().writeValue(sw, client);

    assertEquals(EXPECTED, sw.toString());

  }
}
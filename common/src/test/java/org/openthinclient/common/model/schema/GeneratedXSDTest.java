package org.openthinclient.common.model.schema;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class GeneratedXSDTest {

  private final String path;

  public GeneratedXSDTest(String path) {
    this.path = path;
  }

  @Parameterized.Parameters(name = "Schema: {0}")
  public static String[] schemaFiles() {
    return new String[]{
            "/schemas/browser/schema/application/browser.xml", //
            "/schemas/cmdline/schema/application/cmdline.xml", //
            "/schemas/cups-client/schema/printer/cups-client.xml", //
            "/schemas/desktop/schema/application/desktop.xml", //
            "/schemas/freerdp-git/schema/application/freerdp-git.xml", //
            "/schemas/freerdp-tk/schema/application/freerdp-tk.xml", //
            "/schemas/penmounttouch/schema/device/penmounttouch.xml", //
            "/schemas/printserver/schema/printer/printserver.xml", //
            "/schemas/rdesktop/schema/application/rdesktop.xml", //
            "/schemas/remote-connect/schema/application/remote-connect.xml", //
            "/schemas/smartcard-lite/schema/device/smartcard-lite.xml", //
            "/schemas/tcos-devices/schema/applicationgroup.xml", //
            "/schemas/tcos-devices/schema/client.xml", //
            "/schemas/tcos-devices/schema/device/autologin.xml", //
            "/schemas/tcos-devices/schema/device/display.xml", //
            "/schemas/tcos-devices/schema/device/keyboard.xml", //
            "/schemas/tcos-devices/schema/device/nfs.xml", //
            "/schemas/tcos-devices/schema/hardwaretype.xml", //
            "/schemas/tcos-devices/schema/location.xml", //
            "/schemas/tcos-devices/schema/realm.xml", //
            "/schemas/tcos-devices/schema/user.xml", //
            "/schemas/tcos-devices/schema/usergroup.xml", //
    };
  }

  @Test
  public void compliesToXSD() throws Exception {

    URL schemaFile = getClass().getResource("/schema1.xsd");
    assertNotNull("schema file /schema1.xsd is missing", schemaFile);
    Source xmlFile = new StreamSource(getClass().getResourceAsStream(path));
    SchemaFactory schemaFactory = SchemaFactory
            .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    javax.xml.validation.Schema schema = schemaFactory.newSchema(schemaFile);
    Validator validator = schema.newValidator();
    validator.validate(xmlFile);
    System.out.println(xmlFile.getSystemId() + " is valid");
  }
}

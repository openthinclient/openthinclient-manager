package org.openthinclient.common.model.schema;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.Unmarshaller;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Profile;
import org.xml.sax.InputSource;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * A unit test, verifying that all schemas (as of 18.12.2016) are loadable.
 */
public class SchemaTest {
  private static Mapping MAPPING;

  @BeforeClass
  public static void initMapping() throws Exception {
    MAPPING = new Mapping();
    MAPPING.loadMapping(new InputSource(Node.class
            .getResourceAsStream("schema-mapping.xml")));

  }

  @SuppressWarnings("unchecked")
  protected <T extends Profile> Schema<T> read(String path) throws Exception {

    // this is essentially a copy of AbstractSchemaProvider.loadSchema


    // read using an input source, so that the xml parser wil.
    // use the encoding specification from the file.
    final InputSource source = new InputSource(getClass().getResourceAsStream(path));

    // Create a new Unmarshaller
    final Unmarshaller unmarshaller = new Unmarshaller();
    unmarshaller.setMapping(MAPPING);

    // Unmarshal the schema
    return (Schema<T>) unmarshaller.unmarshal(source);

  }

  @Test
  public void testBrowserSchema() throws Exception {
    final Schema<Application> schema = read("/schemas/browser/schema/application/browser.xml");

    assertEquals("browser", schema.getName());
    assertLabel("en", "Browser", schema);
    assertLabel("de", "Browser", schema);

    GroupNode group = assertGroupNode("General", schema);
    assertLabel("en", "General settings", group);
    assertLabel("de", "Generelle Einstellungen", group);

    final ChoiceNode exitAction = assertChoiceNode("ExitAction", group);

    assertLabel("en", "On application exit", exitAction);
    assertLabel("de", "nach beenden der Anwendung", exitAction);

    {
      final ChoiceNode.Option option = assertOption("Restart application", "Restart", exitAction);
      assertLabel("en", "restart application", option.getLabels());
      assertLabel("de", "Anwendung neustarten", option.getLabels());
    }

  }

  @Test
  public void testCupsClientSchema() throws Exception {
    final Schema<Application> schema = read("/schemas/cups-client/schema/printer/cups-client.xml");

    assertEquals("cups-client", schema.getName());
    assertLabel("en", "CUPS client", schema);
    assertLabel("de", "CUPS client", schema);

    EntryNode host = assertEntryNode("Host", schema);
    assertLabel("en", "CUPS Host", host);
    assertLabel("de", "CUPS Host", host);

    EntryNode port = assertEntryNode("TCPPort", schema);
    assertEquals("631", port.getValue());
    assertLabel("en", "", port);
    assertLabel("de", "Port", port);


  }

  @Test
  public void testRealmSchema() throws Exception {
    final Schema<Application> schema = read("/schemas/tcos-devices/schema/realm.xml");

    assertEquals("realm", schema.getName());

    final SectionNode directory = assertSectionNode("Directory", schema);

    final GroupNode primary = assertGroupNode("Primary", directory);
    final GroupNode readOnly = assertGroupNode("ReadOnly", primary);

    final PasswordNode secret = assertPasswordNode("Secret", readOnly);
    assertEquals("secret", secret.getValue());
  }

  private ChoiceNode.Option assertOption(String name, String value, ChoiceNode node) {

    final Optional<ChoiceNode.Option> option = node.getOptions().stream().filter(opt -> opt.getName().equals(name)).findFirst();

    assertTrue("missing option " + name, option.isPresent());

    assertEquals(name, option.get().getName());
    assertEquals(value, option.get().getValue());

    return option.get();
  }

  private PasswordNode assertPasswordNode(String name, Node parent) {
    return assertChildNode(name, parent, PasswordNode.class);
  }

  private SectionNode assertSectionNode(String name, Node parent) {
    return assertChildNode(name, parent, SectionNode.class);
  }

  private ChoiceNode assertChoiceNode(String name, Node parent) {
    return assertChildNode(name, parent, ChoiceNode.class);
  }

  private EntryNode assertEntryNode(String name, Node parent) {
    return assertChildNode(name, parent, EntryNode.class);
  }

  private GroupNode assertGroupNode(String name, Node parent) {
    return assertChildNode(name, parent, GroupNode.class);
  }

  @SuppressWarnings("unchecked")
  private <T extends Node> T assertChildNode(String name, Node parent, Class<T> clazz) {
    final Node child = parent.getChild(name);

    assertThat(child, CoreMatchers.instanceOf(clazz));
    return (T) child;
  }

  private void assertLabel(String language, String expectedString, Node node) {

    assertLabel(language, expectedString, node.getLabels());
  }

  private void assertLabel(String language, String expectedString, List<Label> labels) {
    final Optional<Label> first = labels.stream().filter(l -> l.getLang().equals(language)).findFirst();

    assertTrue("missing " + language + " label", first.isPresent());
    assertEquals(expectedString, first.get().getLabel());
  }
}
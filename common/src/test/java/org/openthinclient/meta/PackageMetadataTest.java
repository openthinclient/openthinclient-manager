package org.openthinclient.meta;

import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;

import static org.junit.Assert.assertEquals;

public class PackageMetadataTest {

  private static final String SAMPLE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + //
          "<metadata xmlns=\"http://www.openthinclient.org/ns/manager/package/metadata/1.0\">" + //
          "<bookmarks>" + //
          "<bookmark path=\"nfs/root/custom/splashscreens\">" + //
          "<icon>vaadin:desktop</icon>" + //
          "<label lang=\"de\" value=\"Begrüßungsbildschirme\"/>" + //
          "<label lang=\"en\" value=\"Splashscreens\"/>" + //
          "</bookmark>" + //
          "</bookmarks>" + //
          "</metadata>";

  @Test
  public void testSerializeSimpleMetadata() throws JAXBException {

    PackageMetadata metadata = new PackageMetadata();

    final Bookmark bookmark = new Bookmark();

    bookmark.getLabel().add(createLabel("de", "Begrüßungsbildschirme"));
    bookmark.getLabel().add(createLabel("en", "Splashscreens"));
    bookmark.setIcon("vaadin:desktop");
    bookmark.setPath("nfs/root/custom/splashscreens");

    metadata.getBookmarks().add(bookmark);


    final Marshaller m = JAXBContext.newInstance(PackageMetadata.class).createMarshaller();

    final StringWriter sw = new StringWriter();
    m.marshal(new ObjectFactory().createMetadata(metadata), sw);

    assertEquals(SAMPLE, sw.toString());

  }

  @Test
  public void testUnmarshal() {
    final PackageMetadataReader reader = new PackageMetadataReader();
    final PackageMetadata metadata = reader.read(new StreamSource(new StringReader(SAMPLE)));

    assertEquals(1, metadata.getBookmarks().size());
    final Bookmark bookmark = metadata.getBookmarks().get(0);
    assertEquals(2, bookmark.getLabel().size());

    assertEquals("nfs/root/custom/splashscreens", bookmark.getPath());
    assertEquals("vaadin:desktop", bookmark.getIcon());

    assertEquals("de", bookmark.getLabel().get(0).getLang());
    assertEquals("Begrüßungsbildschirme", bookmark.getLabel().get(0).getValue());

    assertEquals("en", bookmark.getLabel().get(1).getLang());
    assertEquals("Splashscreens", bookmark.getLabel().get(1).getValue());
  }

  private Label createLabel(String lang, String value) {
    final Label label = new Label();
    label.setLang(lang);
    label.setValue(value);
    return label;
  }
}
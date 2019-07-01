package org.openthinclient.service.common.home.impl;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openthinclient.service.common.ServerIDFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class XMLManagerHomeMetadataTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testWriteSimpleHomeMetadata() throws Exception {

    final Path dir = tempFolder.getRoot().toPath();
    final XMLManagerHomeMetadata data = XMLManagerHomeMetadata.create(dir);

    assertNull(data.getServerID());

    data.setServerID(ServerIDFactory.create());

    assertNotNull(data.getServerID());
    assertEquals(1, data.getHomeSchemaVersion());

    data.save();

    final Path metadataFile = dir.resolve(XMLManagerHomeMetadata.FILENAME);
    assertTrue(Files.exists(metadataFile));

    Files.readAllLines(metadataFile).forEach(System.out::println);
  }

  @Test
  public void testReadHomeMetadata() throws Exception {
    final XMLManagerHomeMetadata metadata = readClasspathHomeMetadata("metadata-test-01.xml");

    assertEquals(75, metadata.getHomeSchemaVersion());

    assertEquals("d92feeae-3985-40d6-b854-7802487c3b00", metadata.getServerID());
  }

  @Test
  public void testReadHomeWithoutStatisticsBooleanAndExpectCorrectDefault() throws Exception {
    // validating the document only ensures that we're facing a XML-file that actually does not
    // contain the usage-statistics-enabled element
    final Document document = readClasspathHomeMetadataDocument("metadata-test-01.xml");
    assertXpathEmpty("//" + XMLManagerHomeMetadata.ELEMENT_USAGE_STATISTICS_ENABLED, document);


    final XMLManagerHomeMetadata metadata = readClasspathHomeMetadata("metadata-test-01.xml");

    assertTrue("default value has to be true", metadata.isUsageStatisticsEnabled());

  }

  @Test
  public void testReadHomeStatisticsBooleanSetToFalse() throws Exception {
    final Document document = readClasspathHomeMetadataDocument("metadata-test-02.xml");
    assertXpathEquals("false", "//" + XMLManagerHomeMetadata.ELEMENT_USAGE_STATISTICS_ENABLED, document);


    final XMLManagerHomeMetadata metadata = readClasspathHomeMetadata("metadata-test-02.xml");

    assertFalse(metadata.isUsageStatisticsEnabled());

  }

  @Test
  public void testHomeStatisticsDefaultValueNotWritten() throws Exception {

    final Path dir = tempFolder.getRoot().toPath();
    final XMLManagerHomeMetadata metadata = XMLManagerHomeMetadata.create(dir);

    assertTrue(metadata.isUsageStatisticsEnabled());
    metadata.save();

    assertXpathEmpty("//" + XMLManagerHomeMetadata.ELEMENT_USAGE_STATISTICS_ENABLED, readMetadataDocument(dir.resolve(XMLManagerHomeMetadata.FILENAME)));

    // save with false to check that the element will be written in that case
    metadata.setUsageStatisticsEnabled(false);
    metadata.save();

    assertXpathEquals("false", "//" + XMLManagerHomeMetadata.ELEMENT_USAGE_STATISTICS_ENABLED, readMetadataDocument(dir.resolve(XMLManagerHomeMetadata.FILENAME)));

  }

  private Document readMetadataDocument(Path file) throws Exception {
    final Document document;
    try (final InputStream in = Files.newInputStream(file)) {
      document = readMetadataDocument(in);
    }
    return document;
  }

  private void assertXpathEmpty(String xpathExpression, Document document) throws Exception {
    assertEquals("XPath expression " + xpathExpression + " should not yield a result", "", xpathEvaluate(xpathExpression, document));
  }

  private void assertXpathEquals(String expected, String xpathExpression, Document document) throws XPathExpressionException {
    assertEquals(expected, xpathEvaluate(xpathExpression, document));
  }

  private String xpathEvaluate(String xpathExpression, Document document) throws XPathExpressionException {
    return XPathFactory.newInstance().newXPath().evaluate(xpathExpression, document);
  }

  private XMLManagerHomeMetadata readClasspathHomeMetadata(String filename) throws IOException {
    final Path dir = tempFolder.getRoot().toPath();
    Files.copy(getClass().getResourceAsStream(filename), dir.resolve(XMLManagerHomeMetadata.FILENAME));

    return XMLManagerHomeMetadata.read(dir);
  }

  private Document readClasspathHomeMetadataDocument(String filename) throws Exception {
    try (final InputStream is = getClass().getResourceAsStream(filename)) {
      return readMetadataDocument(is);
    }
  }

  private Document readMetadataDocument(InputStream is) throws Exception {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final DocumentBuilder db = dbf.newDocumentBuilder();
    return db.parse(is);
  }


}
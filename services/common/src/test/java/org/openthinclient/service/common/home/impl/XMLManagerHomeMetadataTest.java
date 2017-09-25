package org.openthinclient.service.common.home.impl;

import org.junit.Test;
import org.openthinclient.service.common.ServerIDFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class XMLManagerHomeMetadataTest {

  @Test
  public void testWriteSimpleHomeMetadata() throws Exception {

    final Path dir = createTempDirectory("manager-home-metadata");
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
    final Path dir = createTempDirectory("read-home-metadata");
    Files.copy(getClass().getResourceAsStream("metadata-test-01.xml"), dir.resolve(XMLManagerHomeMetadata.FILENAME));

    final XMLManagerHomeMetadata metadata = XMLManagerHomeMetadata.read(dir);

    assertEquals(75, metadata.getHomeSchemaVersion());

    assertEquals("d92feeae-3985-40d6-b854-7802487c3b00", metadata.getServerID());
  }

  private Path createTempDirectory(String name) throws IOException {
    final Path unitTestDirectory = Paths.get("target", "unit-test");
    Files.createDirectories(unitTestDirectory);
    final Path dir = Files.createTempDirectory(unitTestDirectory, name + "-");
    Files.createDirectories(dir);
    return dir;
  }
}
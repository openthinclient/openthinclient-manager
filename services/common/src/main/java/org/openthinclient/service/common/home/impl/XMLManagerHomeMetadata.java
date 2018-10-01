package org.openthinclient.service.common.home.impl;

import org.openthinclient.service.common.home.ManagerHomeMetadata;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@XmlRootElement(name = "manager-home-metadata", namespace = XMLManagerHomeMetadata.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class XMLManagerHomeMetadata implements ManagerHomeMetadata {

  public static final String FILENAME = ".otc-manager-home.meta";
  public static final String ELEMENT_USAGE_STATISTICS_ENABLED = "usage-statistics-enabled";

  public static boolean exists(Path managerHome) {
    final Path metaFile = managerHome.resolve(FILENAME);
    return Files.exists(metaFile) && Files.isRegularFile(metaFile);
  }

  public static XMLManagerHomeMetadata read(Path managerHome) throws IOException {
    final Path metaFile = managerHome.resolve(FILENAME);

    try(final InputStream in = Files.newInputStream(metaFile)) {
      final XMLManagerHomeMetadata xmlManagerHomeMetadata = read(in);
      xmlManagerHomeMetadata.metaFile = metaFile;
      return xmlManagerHomeMetadata;
    }
  }

  public static XMLManagerHomeMetadata read(InputStream in) {
    return JAXB.unmarshal(in, XMLManagerHomeMetadata.class);
  }

  public static XMLManagerHomeMetadata create(Path managerHome) {
    final XMLManagerHomeMetadata xmlManagerHomeMetadata = new XMLManagerHomeMetadata();
    xmlManagerHomeMetadata.metaFile = managerHome.resolve(FILENAME);
    return xmlManagerHomeMetadata;
  }

  public static final String NAMESPACE = "http://www.openthinclient.org/ns/manager/metadata/1.0";

  @XmlAttribute(name="home-schema-version")
  private int homeSchemaVersion = 1;
  @XmlElement(name = "server-id")
  private String serverId;
  @XmlTransient
  private Path metaFile;
  @XmlElement(name= ELEMENT_USAGE_STATISTICS_ENABLED)
  private Boolean usageStatisticsEnabled;

  @Override
  public String getServerID() {
    return serverId;
  }

  @Override
  public void setServerID(String id) {
    this.serverId = id;
  }

  public int getHomeSchemaVersion() {
    return homeSchemaVersion;
  }

  public void setHomeSchemaVersion(int homeSchemaVersion) {
    this.homeSchemaVersion = homeSchemaVersion;
  }

  public void setUsageStatisticsEnabled(boolean usageStatisticsEnabled) {
    if (usageStatisticsEnabled)
      this.usageStatisticsEnabled = null;
    else
      this.usageStatisticsEnabled = false;
  }

  @Override
  public boolean isUsageStatisticsEnabled() {
    return usageStatisticsEnabled != null ? usageStatisticsEnabled : true;
  }

  @SuppressWarnings("unused")
  void afterUnmarshal(Unmarshaller u, Object parent) {
    if (serverId != null)
      serverId = serverId.trim();
  }

  @Override
  public synchronized void save() {
    try {

      JAXBContext ctx = JAXBContext.newInstance(XMLManagerHomeMetadata.class);
      Marshaller marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      marshaller.marshal(this, Files.newOutputStream(metaFile));
    } catch (Exception e) {
      throw new RuntimeException("Failed to write metadata", e);
    }
  }
}

package org.openthinclient.service.common.home.impl;

import org.openthinclient.service.common.home.ManagerHomeMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "manager-home-metadata", namespace = XMLManagerHomeMetadata.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class XMLManagerHomeMetadata implements ManagerHomeMetadata {

  public static final String FILENAME = ".otc-manager-home.meta";

  public static boolean exists(Path managerHome) {
    final Path metaFile = managerHome.resolve(FILENAME);
    return Files.exists(metaFile) && Files.isRegularFile(metaFile);
  }

  public static XMLManagerHomeMetadata read(Path managerHome) throws IOException {
    final Path metaFile = managerHome.resolve(FILENAME);

    final XMLManagerHomeMetadata xmlManagerHomeMetadata = JAXB.unmarshal(Files.newInputStream(metaFile), XMLManagerHomeMetadata.class);
    xmlManagerHomeMetadata.metaFile = metaFile;
    return xmlManagerHomeMetadata;
  }

  public static XMLManagerHomeMetadata create(Path managerHome) {
    final XMLManagerHomeMetadata xmlManagerHomeMetadata = new XMLManagerHomeMetadata();
    xmlManagerHomeMetadata.metaFile = managerHome.resolve(FILENAME);
    return xmlManagerHomeMetadata;
  }

  public static final String NAMESPACE = "http://www.openthinclient.org/ns/manager/metadata/1.0";

  @XmlElement(name = "server-id")
  private String serverId;
  @XmlTransient
  private Path metaFile;

  @Override
  public String getServerID() {
    return serverId;
  }

  @Override
  public void setServerID(String id) {
    this.serverId = id;
  }

  @Override
  public void save() {
    try {
      JAXB.marshal(this, Files.newOutputStream(metaFile));
    } catch (IOException e) {
      throw new RuntimeException("Failed to write metadata", e);
    }
  }
}

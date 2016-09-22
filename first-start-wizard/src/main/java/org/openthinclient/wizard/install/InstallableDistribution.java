package org.openthinclient.wizard.install;

import org.openthinclient.pkgmgr.SourcesList;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {"description", "sourcesList", "minimumPackages"})
@XmlAccessorType(XmlAccessType.FIELD)
public class InstallableDistribution {

  @XmlElement(name = "sources")
  private final SourcesList sourcesList;
  @XmlElement(name = "install-package")
  private final List<String> minimumPackages;
  /**
   * The preferred attribute identifies the "openthinclient suggested distribution". Typically this
   * flag will be set on the latest distribution available.
   */
  @XmlAttribute
  private boolean preferred;
  @XmlAttribute(name = "name")
  private String name;
  @XmlElement
  private String description;

  public InstallableDistribution() {
    this.sourcesList = new SourcesList();
    minimumPackages = new ArrayList<>();
  }

  public InstallableDistribution(String name, String description) {
    this();
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public SourcesList getSourcesList() {
    return sourcesList;
  }

  public List<String> getMinimumPackages() {
    return minimumPackages;
  }

  public boolean isPreferred() {
    return preferred;
  }

  public void setPreferred(boolean preferred) {
    this.preferred = preferred;
  }
}

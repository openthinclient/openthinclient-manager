package org.openthinclient.api.distributions;

import org.openthinclient.pkgmgr.SourcesList;
import org.openthinclient.pkgmgr.db.Package;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlType(propOrder = {"description", "sourcesList", "minimumPackages", "importItems"})
@XmlAccessorType(XmlAccessType.FIELD)
public class InstallableDistribution {

  @XmlElement(name = "sources")
  private final SourcesList sourcesList;

  /**
   * This is filled by distributions.xml (install-package tag entries) and represents packages that has to be installed
   */
  @XmlElement(name = "install-package")
  private final List<String> minimumPackages;
  @XmlElements({
          @XmlElement(name = "import-application", type = ImportItem.Application.class),
          @XmlElement(name = "import-hardwaretype", type = ImportItem.HardwareType.class),
          @XmlElement(name = "import-device", type = ImportItem.Device.class),
          @XmlElement(name = "import-location", type = ImportItem.Location.class),
          @XmlElement(name = "import-client", type = ImportItem.Client.class),
          @XmlElement(name = "import-printer", type = ImportItem.Printer.class),
  })
  private final List<ImportItem> importItems;
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
  @XmlTransient
  private InstallableDistributions parent;
  /**
   * Additional 'installable' packages can be added via commandline 'PrepareHome'-Command
   */
  @XmlTransient
  private List<Package> additionalPackages;


  public InstallableDistribution() {
    this.sourcesList = new SourcesList();
    minimumPackages = new ArrayList<>(); // filled by distributions.xml
    importItems = new ArrayList<>();
    additionalPackages = new ArrayList<>(); // added by commandline 'PrepareHome'-Command
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

  public List<ImportItem> getImportItems() {
    return importItems;
  }

  /**
   * JAXB callback method, called after unmarshalling. This will method will initiate a backwards
   * reference to the containing parent {@link InstallableDistributions}.
   */
  @SuppressWarnings("unused")
  void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    this.parent = (InstallableDistributions) parent;
  }

  public InstallableDistributions getParent() {
    return parent;
  }

  public List<Package> getAdditionalPackages() {
    return additionalPackages;
  }

  public void setAdditionalPackages(List<Package> additionalPackages) {
    this.additionalPackages = additionalPackages;
  }
}

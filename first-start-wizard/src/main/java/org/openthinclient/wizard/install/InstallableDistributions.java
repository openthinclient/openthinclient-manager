package org.openthinclient.wizard.install;

import org.openthinclient.pkgmgr.SourcesList;
import org.openthinclient.pkgmgr.db.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "distributions")
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class InstallableDistributions {

  public static JAXBContext CONTEXT;

  static {
    try {
      CONTEXT = JAXBContext.newInstance(InstallableDistributions.class, InstallableDistribution.class, SourcesList.class, Source.class);
    } catch (JAXBException e) {
      throw new RuntimeException("Failed to initialize required JAXB configuration", e);
    }
  }

  @XmlElement(name = "distribution")
  private final List<InstallableDistribution> installableDistributions = new ArrayList<>();

  public static InstallableDistributions getDefaultDistributions() {
    try {
      final Unmarshaller unmarshaller = CONTEXT.createUnmarshaller();

      return (InstallableDistributions) unmarshaller.unmarshal(InstallableDistributions.class.getResourceAsStream("/org/openthinclient/distributions.xml"));
    } catch (Exception e) {
      throw new RuntimeException("Failed to load the default distributions", e);
    }
  }

  /**
   * Returns the most preferred distribution for installation.
   */
  public static InstallableDistribution getPreferredDistribution() {
    final List<InstallableDistribution> distributions = getDefaultDistributions().getInstallableDistributions();
    Optional<InstallableDistribution> first = distributions.stream().filter(InstallableDistribution::isPreferred).findFirst();

    // either return the first preferred, the first in the list or null. Depending on which rule applies first.
    return first.orElseGet(() -> distributions.stream().findFirst().orElse(null));
  }

  public List<InstallableDistribution> getInstallableDistributions() {
    return installableDistributions;
  }
}

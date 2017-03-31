package org.openthinclient.api.distributions;

import org.openthinclient.pkgmgr.SourcesList;
import org.openthinclient.pkgmgr.db.Source;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@XmlRootElement(name = "distributions")
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class InstallableDistributions {

  public static final URI OFFICIAL_DISTRIBUTIONS_XML = URI.create("http://archive.openthinclient.org/openthinclient/distributions.xml");
  public static final String LOCAL_DISTRIBUTIONS_XML = "/org/openthinclient/distributions.xml";

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
  @XmlTransient
  private URI baseURI;

  public static InstallableDistributions getDefaultDistributions() {
    try {
      return load(getDefaultDistributionsURL());
    } catch (Exception e) {
      throw new RuntimeException("Failed to load the default distributions", e);
    }
  }

  /**
   * Returns the packaged default <code>distributions.xml</code> URL. This URL will be used to
   * resolve the fallback and pre-packaged <code>distributions.xml</code> in the case that the
   * {@link #OFFICIAL_DISTRIBUTIONS_XML official one} is not reachable.
   */
  public static URL getDefaultDistributionsURL() {
    return InstallableDistributions.class.getResource(LOCAL_DISTRIBUTIONS_XML);
  }

  public static InstallableDistributions load(URL distributionsURL, Proxy proxy) throws Exception {
    final URI distributionsURI = distributionsURL.toURI();
    try (final InputStream is = distributionsURL.openConnection(proxy).getInputStream()) {
      return load(distributionsURI, is);
    }
  }

  public static InstallableDistributions load(URL distributionsURL) throws Exception {
    final URI distributionsURI = distributionsURL.toURI();
    try (final InputStream is = distributionsURL.openStream()) {
      return load(distributionsURI, is);
    }
  }

  /**
   * Load a <code>distributions.xml</code> from the provided {@link InputStream} using the
   * <code>distributionsURI</code> to resolve the resulting {@link #setBaseURI(URI) baseURI}
   */
  public static InstallableDistributions load(URI distributionsURI, InputStream is) throws Exception {
    final Unmarshaller unmarshaller = CONTEXT.createUnmarshaller();
    final InstallableDistributions installableDistributions = (InstallableDistributions) unmarshaller.unmarshal(is);

    // the distributionsURI is expected to point to a file like distributions.xml.
    // Resolving that URI against a '.' (current directory), will essentially remove the last path segment of the URI.
    // Example:
    // distributionsURI: http://archive.openthinclient.org/openthinclient/distributions.xml
    // baseURI: http://archive.openthinclient.org/openthinclient/
    final URI baseURI = distributionsURI.resolve(".");
    installableDistributions.setBaseURI(baseURI);
    return installableDistributions;
  }

  /**
   * Returns the most preferred distribution for installation.
   */
  public static InstallableDistribution getPreferredDistribution() {
    return getDefaultDistributions().getPreferred();
  }

  public List<InstallableDistribution> getInstallableDistributions() {
    return installableDistributions;
  }

  public URI getBaseURI() {
    return baseURI;
  }

  public void setBaseURI(URI baseURI) {
    this.baseURI = baseURI;
  }

  public InstallableDistribution getPreferred() {
    Optional<InstallableDistribution> first = installableDistributions.stream().filter(InstallableDistribution::isPreferred).findFirst();

    // either return the first preferred, the first in the list or null. Depending on which rule applies first.
    return first.orElseGet(() -> installableDistributions.stream().findFirst().orElse(null));
  }
}

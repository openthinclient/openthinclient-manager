package org.openthinclient.wizard.model;

import org.openthinclient.pkgmgr.Source;
import org.openthinclient.wizard.install.InstallableDistribution;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class InstallModel {

  private final List<InstallableDistribution> installableDistributions;

  public InstallModel() {
    installableDistributions = new ArrayList<>();

    final InstallableDistribution distribution = new InstallableDistribution("openthinclient consus", "Version 2 of the openthinclient operating system");

    distribution.getMinimumPackages().add("base");
    final Source source = new Source();
    source.setType(Source.Type.PACKAGE);
    source.setDescription("Rolling");
    source.setEnabled(true);
    try {
      source.setUrl(new URL("http://archive.openthinclient.org/openthinclient/v2/manager-rolling"));
    } catch (MalformedURLException e) {
      // this exception should not happen, as the url is specified above
      throw new RuntimeException("Failed to create URL instance");
    }
    source.setDistribution("./");
    distribution.getSourcesList().getSources().add(source);

    installableDistributions.add(distribution);

  }

  public List<InstallableDistribution> getInstallableDistributions() {
    return installableDistributions;
  }
}

package org.openthinclient.wizard.install;

import org.openthinclient.pkgmgr.SourcesList;

import java.util.ArrayList;
import java.util.List;

public class InstallableDistribution {

  private final String name;
  private final String description;
  private final SourcesList sourcesList;
  private final List<String> minimumPackages;

  public InstallableDistribution(String name, String description) {

    this.name = name;
    this.description = description;
    this.sourcesList = new SourcesList();
    minimumPackages = new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public SourcesList getSourcesList() {
    return sourcesList;
  }

  public List<String> getMinimumPackages() {
    return minimumPackages;
  }
}

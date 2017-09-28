package org.openthinclient.sysreport;

import java.util.ArrayList;
import java.util.List;

public final class PackageManagerSubsystem extends SystemReport.Subsystem {

  private final List<Package> installed;
  private final List<Package> installable;

  public PackageManagerSubsystem() {
    super("package-manager");
    installable = new ArrayList<>();
    installed = new ArrayList<>();
  }

  public List<Package> getInstallable() {
    return installable;
  }

  public List<Package> getInstalled() {
    return installed;
  }

}

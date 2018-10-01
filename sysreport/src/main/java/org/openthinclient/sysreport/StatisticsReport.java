package org.openthinclient.sysreport;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * A report similar to {@link SystemReport} but with the intention to collect anonymous usage
 * statistics instead of a full-blown system report for support.
 */
public class StatisticsReport extends AbstractReport {

  // keeping a report version property which will be incremented once massive changes occur.
  @JsonProperty("report-version")
  private final int reportVersion = 1;

  private final Network network;
  @JsonProperty("package-manager")
  private final PackageManagerSummary packageManager;

  public StatisticsReport() {
    this.network = new Network();
    packageManager = new PackageManagerSummary();
  }

  public PackageManagerSummary getPackageManager() {
    return packageManager;
  }

  public Network getNetwork() {
    return network;
  }

  public static class Network {
    private final List<NetworkInterface> interfaces = new ArrayList<>();

    public List<NetworkInterface> getInterfaces() {
      return interfaces;
    }
  }

  public static class PackageManagerSummary {

    private final List<PackageSummary> installed;
    private final List<Source> sources;

    public PackageManagerSummary() {
      installed = new ArrayList<>();
      sources = new ArrayList<>();
    }

    public List<PackageSummary> getInstalled() {
      return installed;
    }

    public List<Source> getSources() {
      return sources;
    }
  }

  public static class PackageSummary {
    private final String name;
    private final String version;

    public PackageSummary(String name, String version) {
      this.name = name;
      this.version = version;
    }

    public String getName() {
      return name;
    }

    public String getVersion() {
      return version;
    }
  }

}

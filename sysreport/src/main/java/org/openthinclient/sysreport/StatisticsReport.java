package org.openthinclient.sysreport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * A report similar to {@link SystemReport} but with the intention to collect anonymous usage
 * statistics instead of a full-blown system report for support.
 */
// ensuring that the report-version property will be serialized first
@JsonPropertyOrder({"report-version"})
public class StatisticsReport extends AbstractReport {

  // keeping a report version property which will be incremented once massive changes occur.
  @JsonProperty("report-version")
  private final int reportVersion = 1;

  private final Network network;
  @JsonProperty("package-manager")
  private final PackageManagerSummary packageManager;

  @JsonProperty("configuration")
  private final ConfigurationSummary configuration;

  public ConfigurationSummary getConfiguration() {
    return configuration;
  }

  public StatisticsReport() {
    this.network = new Network();
    packageManager = new PackageManagerSummary();
    configuration = new ConfigurationSummary();
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

  public static class ConfigurationSummary {
    @JsonProperty("thinclient-count")
    private int thinClientCount;
    @JsonProperty("application-count")
    private int applicationCount;
    @JsonProperty("application-group-count")
    private int applicationGroupCount;
    @JsonProperty("thinclient-group-count")
    private int thinClientGroupCount;

    public int getApplicationCount() {
      return applicationCount;
    }

    public void setApplicationCount(int applicationCount) {
      this.applicationCount = applicationCount;
    }

    public int getApplicationGroupCount() {
      return applicationGroupCount;
    }

    public void setApplicationGroupCount(int applicationGroupCount) {
      this.applicationGroupCount = applicationGroupCount;
    }

    public int getThinClientGroupCount() {
      return thinClientGroupCount;
    }

    public void setThinClientGroupCount(int thinClientGroupCount) {
      this.thinClientGroupCount = thinClientGroupCount;
    }

    public int getThinClientCount() {
      return thinClientCount;
    }

    public void setThinClientCount(int thinClientCount) {
      this.thinClientCount = thinClientCount;
    }
  }

}

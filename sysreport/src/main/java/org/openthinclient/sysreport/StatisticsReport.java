package org.openthinclient.sysreport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

  @JsonProperty("created")
  private Long created;

  private final Network network;
  @JsonProperty("package-manager")
  private final PackageManagerSummary packageManager;

  @JsonProperty("configuration")
  private final ConfigurationSummary configuration;

  public StatisticsReport() {
    this.created = OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond();
    this.network = new Network();
    packageManager = new PackageManagerSummary();
    configuration = new ConfigurationSummary();
  }

  public ConfigurationSummary getConfiguration() {
    return configuration;
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
    @JsonProperty("unregistered-count")
    private int unrecognizedClientCount;
    @JsonProperty("application-group-count")
    private int applicationGroupCount;
    @JsonProperty("thinclient-group-count")
    private int thinClientGroupCount;

    public Map<String, Long> getApplications() {
      return applications;
    }

    public void setApplications(Map<String, Long> applications) {
      this.applications = applications;
    }

    @JsonProperty
    private Map<String, Long> applications = new TreeMap<>();

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

    public int getUnrecognizedClientCount() {
      return unrecognizedClientCount;
    }

    public void setUnrecognizedClientCount(int unrecognizedClientCount) {
      this.unrecognizedClientCount = unrecognizedClientCount;
    }

    public int getThinClientCount() {
      return thinClientCount;
    }

    public void setThinClientCount(int thinClientCount) {
      this.thinClientCount = thinClientCount;
    }
  }

}

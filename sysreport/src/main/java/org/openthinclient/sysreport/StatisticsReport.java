package org.openthinclient.sysreport;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;
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
    @JsonProperty("secondary-ldap-active")
    private boolean secondaryLdapActive;
    @JsonProperty("primary-ldap-user-count")
    private int primaryLdapUserCount;
    @JsonProperty("primary-ldap-user-group-count")
    private int primaryLdapUserGroupCount;
    @JsonProperty
    private Map<String, Long> applications = new TreeMap<>();
    @JsonProperty
    private Map<String, Long> applicationUsage = new TreeMap<>();
    @JsonProperty
    private Map<String, Long> devices = new TreeMap<>();
    @JsonProperty
    private Map<String, Long> deviceUsage = new TreeMap<>();
    @JsonProperty
    private Map<String, Long> locations = new TreeMap<>();
    @JsonProperty
    private Map<String, Long> locationUsage = new TreeMap<>();
    @JsonProperty
    private Map<String, Long> printers = new TreeMap<>();
    @JsonProperty
    private Map<String, Long> printerUsage = new TreeMap<>();
    @JsonProperty
    private Map<String, Long> hardwaretypes = new TreeMap<>();
    @JsonProperty
    private Map<String, Long> hardwaretypeUsage = new TreeMap<>();
    @JsonProperty
    Integer licenseCount;
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate licenseSoftExpiredDate;
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate licenseExpiredDate;
    @JsonProperty
    String licenseState;

    @JsonProperty("admin-user-login-count")
    private int adminUserLoginCount;

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

    public boolean isSecondaryLdapActive() {
      return secondaryLdapActive;
    }

    public void setSecondaryLdapActive(boolean secondaryLdapActive) {
      this.secondaryLdapActive = secondaryLdapActive;
    }

    public Map<String, Long> getApplications() {
      return applications;
    }

    public void setApplications(Map<String, Long> applications) {
      this.applications = applications;
    }

    public int getPrimaryLdapUserCount() {
      return primaryLdapUserCount;
    }

    public void setPrimaryLdapUserCount(int primaryLdapUserCount) {
      this.primaryLdapUserCount = primaryLdapUserCount;
    }

    public int getPrimaryLdapUserGroupCount() {
      return primaryLdapUserGroupCount;
    }

    public void setPrimaryLdapUserGroupCount(int primaryLdapUserGroupCount) {
      this.primaryLdapUserGroupCount = primaryLdapUserGroupCount;
    }

    public Map<String, Long> getApplicationUsage() {
      return applicationUsage;
    }

    public void setApplicationUsage(Map<String, Long> applicationUsage) {
      this.applicationUsage = applicationUsage;
    }

    public Map<String, Long> getDeviceUsage() {
      return deviceUsage;
    }

    public Map<String, Long> getLocationUsage() {
      return locationUsage;
    }

    public void setLocationUsage(Map<String, Long> locationUsage) {
      this.locationUsage = locationUsage;
    }

    public Map<String, Long> getPrinterUsage() {
      return printerUsage;
    }

    public void setPrinterUsage(Map<String, Long> printerUsage) {
      this.printerUsage = printerUsage;
    }

    public void setDeviceUsage(Map<String, Long> deviceUsage) {
      this.deviceUsage = deviceUsage;
    }

    public Map<String, Long> getHardwaretypeUsage() {
      return hardwaretypeUsage;
    }

    public void setHardwaretypeUsage(Map<String, Long> hardwaretypeUsage) {
      this.hardwaretypeUsage = hardwaretypeUsage;
    }

    public Map<String, Long> getDevices() {
      return devices;
    }

    public void setDevices(Map<String, Long> devices) {
      this.devices = devices;
    }

    public Map<String, Long> getLocations() {
      return locations;
    }

    public void setLocations(Map<String, Long> locations) {
      this.locations = locations;
    }

    public Map<String, Long> getPrinters() {
      return printers;
    }

    public void setPrinters(Map<String, Long> printers) {
      this.printers = printers;
    }

    public Map<String, Long> getHardwaretypes() {
      return hardwaretypes;
    }

    public void setHardwaretypes(Map<String, Long> hardwaretypes) {
      this.hardwaretypes = hardwaretypes;
    }

    public Integer getLicenseCount() {
      return licenseCount;
    }

    public void setLicenseCount(Integer licenseCount) {
      this.licenseCount = licenseCount;
    }

    public LocalDate getLicenseSoftExpiredDate() {
      return licenseSoftExpiredDate;
    }

    public void setLicenseSoftExpiredDate(LocalDate licenseSoftExpiredDate) {
      this.licenseSoftExpiredDate = licenseSoftExpiredDate;
    }

    public LocalDate getLicenseExpiredDate() {
      return licenseExpiredDate;
    }

    public void setLicenseExpiredDate(LocalDate licenseExpiredDate) {
      this.licenseExpiredDate = licenseExpiredDate;
    }

    public String getLicenseState() {
      return licenseState;
    }

    public void setLicenseState(String licenseState) {
      this.licenseState = licenseState;
    }
  }

}

package org.openthinclient.sysreport;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class AbstractReport {
  protected final Server server;
  protected final Manager manager;

  public AbstractReport() {
    server = new Server();
    manager = new Manager();
  }

  public Server getServer() {
    return server;
  }

  public Manager getManager() {
    return manager;
  }

  public static final class Java {

    private Map<String, String> properties;
    private List<String> propertyKeys;

    public List<String> getPropertyKeys() {
      return propertyKeys;
    }

    public void setPropertyKeys(List<String> propertyKeys) {
      this.propertyKeys = propertyKeys;
    }

    public Map<String, String> getProperties() {
      return properties;
    }

    public void setProperties(Map<String, String> properties) {
      this.properties = properties;
    }
  }

  public static final class OperatingSystem {
    private String arch;
    private String name;
    private String version;

    public String getArch() {
      return arch;
    }

    public void setArch(String arch) {
      this.arch = arch;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }
  }

  public static final class Server {
    private final OperatingSystem os = new OperatingSystem();
    private String ServerId;
    private long freeDiskSpace;
    private Map<String, String> environment;
    @JsonProperty("environment-keys")
    private List<String> environmentKeys;

    public String getServerId() {
      return ServerId;
    }

    public void setServerId(String serverId) {
      ServerId = serverId;
    }

    public long getFreeDiskSpace() {
      return freeDiskSpace;
    }

    public void setFreeDiskSpace(long freeDiskSpace) {
      this.freeDiskSpace = freeDiskSpace;
    }

    public OperatingSystem getOS() {
      return os;
    }

    public Map<String, String> getEnvironment() {
      return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
      this.environment = environment;
    }

    public List<String> getEnvironmentKeys() {
      return environmentKeys;
    }

    public void setEnvironmentKeys(List<String> environmentKeys) {
      this.environmentKeys = environmentKeys;
    }
  }

  public static final class Manager {
    private final Java java = new Java();
    private String version;

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public Java getJava() {
      return java;
    }
  }

  public static class NetworkInterface {
    @JsonProperty("display-name")
    private String displayName;
    private String name;

    public String getDisplayName() {
      return displayName;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}

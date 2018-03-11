package org.openthinclient.sysreport;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SystemReport {

  private final Submitter submitter;
  private final Manager manager;
  private final Server server;
  private final Network network;
  @JsonProperty("package-management")
  private final PackageManagerSubsystem packageManager;

  public SystemReport() {
    submitter = new Submitter();
    manager = new Manager();
    server = new Server();
    network = new Network();
    packageManager = new PackageManagerSubsystem();
  }

  public Network getNetwork() {
    return network;
  }

  public Server getServer() {
    return server;
  }

  public Manager getManager() {
    return manager;
  }

  public PackageManagerSubsystem getPackageManager() {
    return packageManager;
  }

  public Submitter getSubmitter() {
    return submitter;
  }

  public enum SubmitterType {
    PERSON, AUTOMATED
  }

  public static final class Submitter {
    @JsonProperty("type")
    private SubmitterType submitterType;
    private String name;
    private String email;

    public SubmitterType getSubmitterType() {
      return submitterType;
    }

    public void setSubmitterType(SubmitterType submitterType) {
      this.submitterType = submitterType;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }
  }

  public static final class Java {

    private Map<String, String> properties;

    public Map<String, String> getProperties() {
      return properties;
    }

    public void setProperties(Map<String, String> properties) {
      this.properties = properties;
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

  public static final class NetworkInterface {
    private final List<String> addresses;
    @JsonProperty("display-name")
    private String displayName;
    private String name;
    @JsonProperty("hardware-address")
    private String hardwareAddress;

    public NetworkInterface() {
      addresses = new ArrayList<>();
    }

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

    public String getHardwareAddress() {
      return hardwareAddress;
    }

    public void setHardwareAddress(String hardwareAddress) {
      this.hardwareAddress = hardwareAddress;
    }

    public List<String> getAddresses() {
      return addresses;
    }
  }

  public static final class Server {
    private final OperatingSystem os = new OperatingSystem();
    private String ServerId;
    private long freeDiskSpace;
    private Map<String, String> environment;

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
  }

  public static final class Network {
    private final List<NetworkInterface> interfaces;
    private final Proxy proxy;

    public Network() {
      proxy = new Proxy();
      interfaces = new ArrayList<>();
    }

    public List<NetworkInterface> getInterfaces() {
      return interfaces;
    }

    public Proxy getProxy() {
      return proxy;
    }

    public static final class Proxy {

      private int port;
      private String user;
      private boolean passwordSpecified;
      private String host;
      private boolean enabled;

      public int getPort() {
        return port;
      }

      public void setPort(int port) {
        this.port = port;
      }

      public String getUser() {
        return user;
      }

      public void setUser(String user) {
        this.user = user;
      }

      public boolean isPasswordSpecified() {
        return passwordSpecified;
      }

      public void setPasswordSpecified(boolean passwordSpecified) {
        this.passwordSpecified = passwordSpecified;
      }

      public String getHost() {
        return host;
      }

      public void setHost(String host) {
        this.host = host;
      }

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }
    }
  }

}

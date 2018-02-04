package org.openthinclient.sysreport;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SystemReport {

  private final Manager manager;
  private final Server server;
  private final Network network;
  @JsonProperty("package-management")
  private final PackageManagerSubsystem packageManager;

  public SystemReport() {
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

  public static final class Manager {
    private String version;

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }
  }

  public static final class Server {
    private String ServerId;
    private long freeDiskSpace;

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
  }

  public static final class Network {

    private final Proxy proxy;

    public Network() {
      proxy = new Proxy();
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

package org.openthinclient.sysreport;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class SystemReport extends AbstractReport {

  protected final Network network;
  private final Submitter submitter;
  @JsonProperty("package-management")
  private final PackageManagerSubsystem packageManager;

  public SystemReport() {
    super();
    submitter = new Submitter();
    packageManager = new PackageManagerSubsystem();
    network = new Network();
  }

  public PackageManagerSubsystem getPackageManager() {
    return packageManager;
  }

  public Submitter getSubmitter() {
    return submitter;
  }

  public Network getNetwork() {
    return network;
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

  public static final class NetworkInterfaceDetails extends NetworkInterface {
    private final List<String> addresses;
    @JsonProperty("hardware-address")
    private String hardwareAddress;

    public NetworkInterfaceDetails() {
      addresses = new ArrayList<>();
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

  public static final class Network {
    private final List<NetworkInterfaceDetails> interfaces;
    private final Proxy proxy;

    public Network() {
      proxy = new Proxy();
      interfaces = new ArrayList<>();
    }

    public List<NetworkInterfaceDetails> getInterfaces() {
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

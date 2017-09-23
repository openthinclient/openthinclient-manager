package org.openthinclient.web.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SystemReport {

  private final List<Subsystem> subsystems;
  private final Manager manager;
  private final Server server;

  public SystemReport() {
    subsystems = new ArrayList<>();
    manager = new Manager();
    server = new Server();
  }

  public Server getServer() {
    return server;
  }

  public Manager getManager() {
    return manager;
  }

  public List<Subsystem> getSubsystems() {
    return Collections.unmodifiableList(subsystems);
  }

  public void addSubsystem(Subsystem subsystem) {
    this.subsystems.add(subsystem);
  }

  public static abstract class Subsystem {

    private final String name;

    public Subsystem(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

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

    public void setFreeDiskSpace(long freeDiskSpace) {
      this.freeDiskSpace = freeDiskSpace;
    }

    public long getFreeDiskSpace() {
      return freeDiskSpace;
    }
  }
}

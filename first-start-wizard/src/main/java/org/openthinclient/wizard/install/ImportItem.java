package org.openthinclient.wizard.install;

import org.openthinclient.api.importer.model.ImportableClient;
import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.api.importer.model.ImportableLocation;
import org.openthinclient.api.rest.model.AbstractProfileObject;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class ImportItem {

  @XmlAttribute
  private final String path;

  @XmlTransient
  private final Class<? extends AbstractProfileObject> targetType;

  public ImportItem(Class<? extends AbstractProfileObject> targetType, String path) {
    this.targetType = targetType;
    this.path = path;
  }

  public String getPath() {
    return path;
  }

  public static class Application extends ImportItem {
    /**
     * For deserialization only
     */
    @Deprecated
    public Application() {
      super(org.openthinclient.api.rest.model.Application.class, null);
    }

    public Application(String path) {
      super(org.openthinclient.api.rest.model.Application.class, path);
    }
  }

  public static class HardwareType extends ImportItem {
    /**
     * For deserialization only
     */
    @Deprecated
    public HardwareType() {
      super(ImportableHardwareType.class, null);
    }

    public HardwareType(String path) {
      super(ImportableHardwareType.class, path);
    }
  }

  public static class Device extends ImportItem {
    /**
     * For deserialization only
     */
    @Deprecated
    public Device() {
      super(org.openthinclient.api.rest.model.Device.class, null);
    }

    public Device(String path) {
      super(org.openthinclient.api.rest.model.Device.class, path);
    }
  }

  public static class Location extends ImportItem {
    /**
     * For deserialization only
     */
    @Deprecated
    public Location() {
      super(ImportableLocation.class, null);
    }

    public Location(String path) {
      super(ImportableLocation.class, path);
    }
  }

  public static class Client extends ImportItem {
    /**
     * For deserialization only
     */
    @Deprecated
    public Client() {
      super(ImportableClient.class, null);
    }

    public Client(String path) {
      super(ImportableClient.class, path);
    }
  }

  public static class Printer extends ImportItem {
    /**
     * For deserialization only
     */
    @Deprecated
    public Printer() {
      super(org.openthinclient.api.rest.model.Printer.class, null);
    }

    public Printer(String path) {
      super(org.openthinclient.api.rest.model.Printer.class, path);
    }
  }

  public Class<? extends AbstractProfileObject> getTargetType() {
    return targetType;
  }

  @Override
  public String toString() {
    return "ImportItem{" +
            "targetType='" + targetType + "'," +
            "path='" + path + '\'' +
            '}';
  }
}

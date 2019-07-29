package org.openthinclient.db;

import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ConfigurationFile;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@ConfigurationFile("db.xml")
@XmlRootElement(name = "database", namespace = "http://www.openthinclient.org/ns/manager/database/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class DatabaseConfiguration implements Configuration {

  @XmlElement
  private DatabaseType type;
  @XmlElement
  private String url;
  @XmlElement
  private String username;
  @XmlElement
  private String password;
  @XmlElement
  private String timezone;

  public DatabaseType getType() {
    return type;
  }

  public void setType(DatabaseType type) {
    this.type = type;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public enum DatabaseType {
    APACHE_DERBY("org.apache.derby.jdbc.EmbeddedDriver", true), 
    MYSQL("com.mysql.jdbc.Driver", false), 
    H2("org.h2.Driver", true);

    private final String driverClassName;
    private final boolean embedded;

    DatabaseType(String driverClassName, boolean embedded) {
      this.driverClassName = driverClassName;
      this.embedded = embedded;
    }

    public String getDriverClassName() {
      return driverClassName;
    }

    public boolean isDriverAvailable() {

      try {
        Class.forName(driverClassName);
        return true;
      } catch (ClassNotFoundException e) {
        return false;
      }

    }

    /**
     * determines whether or not the database is run embedded in the manager application or not.
     */
    public boolean isEmbedded() {
      return embedded;
    }
  }
}

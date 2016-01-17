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
      MYSQL("com.mysql.jdbc.Driver");

      private final String driverClassName;

      DatabaseType(String driverClassName) {this.driverClassName = driverClassName;}

      public String getDriverClassName() {
         return driverClassName;
      }
   }
}

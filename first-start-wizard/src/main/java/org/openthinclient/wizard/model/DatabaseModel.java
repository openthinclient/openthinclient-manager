package org.openthinclient.wizard.model;

import org.openthinclient.db.DatabaseConfiguration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class DatabaseModel {

   public DatabaseConfiguration.DatabaseType getType() {
      return type;
   }

   public void setType(DatabaseConfiguration.DatabaseType type) {
      this.type = type;
   }

   @NotNull
   private DatabaseConfiguration.DatabaseType type = DatabaseConfiguration.DatabaseType.H2;
   private final MySQLConfiguration mySQLConfiguration;

   public DatabaseModel() {mySQLConfiguration = new MySQLConfiguration();}

   public MySQLConfiguration getMySQLConfiguration() {
      return mySQLConfiguration;
   }

   public static class MySQLConfiguration {
      @NotNull
      private String hostname = "localhost";
      @NotNull
      private String username = "root";

      private String password;
      @NotNull
      private String database = "openthinclient";
      @Min(1)
      @Max(65535)
      private int port = 3306;

      public String getHostname() {
         return hostname;
      }

      public void setHostname(String hostname) {
         this.hostname = hostname;
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

      public String getDatabase() {
         return database;
      }

      public void setDatabase(String database) {
         this.database = database;
      }

      public int getPort() {
         return port;
      }

      public void setPort(int port) {
         this.port = port;
      }
   }

}

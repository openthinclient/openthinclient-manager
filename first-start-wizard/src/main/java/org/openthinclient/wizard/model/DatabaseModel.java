package org.openthinclient.wizard.model;

import org.openthinclient.db.DatabaseConfiguration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class DatabaseModel {

  private final MySQLConfiguration mySQLConfiguration;
  @NotNull
  private DatabaseConfiguration.DatabaseType type = DatabaseConfiguration.DatabaseType.H2;

  public DatabaseModel() {
    mySQLConfiguration = new MySQLConfiguration();
  }

  /**
   * Apply the given {@link DatabaseModel database model} to the {@link DatabaseConfiguration}.
   *
   * @param model  the source {@link DatabaseModel}
   * @param target the {@link DatabaseConfiguration} that the data shall be applied to
   */
  public static void apply(DatabaseModel model, DatabaseConfiguration target) {
    target.setType(model.getType());
    if (model.getType() == DatabaseConfiguration.DatabaseType.MYSQL) {

      final MySQLConfiguration mySQLConfiguration = model.getMySQLConfiguration();
      target.setUrl("jdbc:mysql://" + mySQLConfiguration.getHostname() + ":" + mySQLConfiguration.getPort() + "/" + mySQLConfiguration.getDatabase());
      target.setUsername(mySQLConfiguration.getUsername());
      target.setPassword(mySQLConfiguration.getPassword());
    } else if (model.getType() == DatabaseConfiguration.DatabaseType.H2) {
      target.setUrl(null);
      target.setUsername("sa");
      target.setPassword("");
    } else {
      throw new IllegalArgumentException("Unsupported type of database " + model.getType());
    }
  }

  public DatabaseConfiguration.DatabaseType getType() {
      return type;
   }

   public void setType(DatabaseConfiguration.DatabaseType type) {
      this.type = type;
   }

<<<<<<< HEAD
   @NotNull
   private DatabaseConfiguration.DatabaseType type = DatabaseConfiguration.DatabaseType.H2;
   private final MySQLConfiguration mySQLConfiguration;
   
   public DatabaseModel() {mySQLConfiguration = new MySQLConfiguration();}

  public MySQLConfiguration getMySQLConfiguration() {
=======
   public MySQLConfiguration getMySQLConfiguration() {
>>>>>>> 41aa1d072808a1fba403d975ca65b656ffd279e6
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

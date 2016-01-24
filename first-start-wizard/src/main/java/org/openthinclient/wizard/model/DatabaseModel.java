package org.openthinclient.wizard.model;

import org.openthinclient.db.DatabaseConfiguration;

public class DatabaseModel {

   private final DatabaseConfiguration databaseConfiguration;

   public DatabaseModel() {databaseConfiguration = new DatabaseConfiguration();}

   public DatabaseConfiguration getDatabaseConfiguration() {
      return databaseConfiguration;
   }
}

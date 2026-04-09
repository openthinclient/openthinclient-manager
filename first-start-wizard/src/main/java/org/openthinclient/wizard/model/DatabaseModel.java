package org.openthinclient.wizard.model;

import org.openthinclient.db.DatabaseConfiguration;

import javax.validation.constraints.NotNull;

public class DatabaseModel {

  @NotNull
  private DatabaseConfiguration.DatabaseType type = DatabaseConfiguration.DatabaseType.APACHE_DERBY;

  /**
   * Apply the given {@link DatabaseModel database model} to the {@link DatabaseConfiguration}.
   * New installations always use the built-in Apache Derby database.
   *
   * @param model  the source {@link DatabaseModel}
   * @param target the {@link DatabaseConfiguration} that the data shall be applied to
   */
  public static void apply(DatabaseModel model, DatabaseConfiguration target) {
    target.setType(DatabaseConfiguration.DatabaseType.APACHE_DERBY);
    target.setUrl(null);
    target.setUsername("sa");
    target.setPassword("");
  }

}

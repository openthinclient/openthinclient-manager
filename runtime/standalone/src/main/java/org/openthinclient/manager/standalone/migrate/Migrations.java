package org.openthinclient.manager.standalone.migrate;

import org.openthinclient.manager.standalone.migrate.impl.CreateServerIDMigration;
import org.openthinclient.manager.standalone.migrate.impl.EncryptProxyUserPasswordMigration;
import org.openthinclient.service.common.home.ManagerHome;

/**
 * Utility class containing the entry point to {@link EarlyMigration migrations}.
 */
public class Migrations {

  public static final EarlyMigration[] EARLY_MIGRATIONS = {
          new CreateServerIDMigration(),
          new EncryptProxyUserPasswordMigration()
  };

  public static void runEarlyMigrations(ManagerHome managerHome) {
    for (EarlyMigration migration : EARLY_MIGRATIONS) {
      migration.migrate(managerHome);
    }
  }

}

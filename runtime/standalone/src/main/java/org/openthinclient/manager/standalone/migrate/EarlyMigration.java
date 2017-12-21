package org.openthinclient.manager.standalone.migrate;

import org.openthinclient.service.common.home.ManagerHome;

/**
 * Callback interface for early (mostly pre-startup migrations).
 */
public interface EarlyMigration {
  void migrate(ManagerHome managerHome);
}

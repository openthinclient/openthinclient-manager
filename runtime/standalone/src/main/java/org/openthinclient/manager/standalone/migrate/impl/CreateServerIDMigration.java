package org.openthinclient.manager.standalone.migrate.impl;

import com.google.common.base.Strings;

import org.openthinclient.manager.standalone.migrate.EarlyMigration;
import org.openthinclient.service.common.ServerIDFactory;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.ManagerHomeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateServerIDMigration implements EarlyMigration {
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateServerIDMigration.class);

  @Override
  public void migrate(ManagerHome managerHome) {

    final ManagerHomeMetadata meta = managerHome.getMetadata();
    if (Strings.isNullOrEmpty(meta.getServerID())) {
      meta.setServerID(ServerIDFactory.create());
      meta.save();

    }
    LOGGER.info("\n#########################################################\n" +
            "# Server ID: " + meta.getServerID() + "\n" +
            "#########################################################");

  }
}

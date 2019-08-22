package org.openthinclient.service.update;

import org.openthinclient.pkgmgr.PackageManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.ApplicationContext;

@Configuration
public class UpdateCheckerConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCheckerConfiguration.class);

  @Autowired
  UpdateChecker updateChecker;
  @Autowired
  private PackageManager packageManager;

  @Bean
  public UpdateChecker updateChecker() {
    return new UpdateChecker();
  }

  @Scheduled(fixedRate = 12 * 60 * 60 * 1000, initialDelay = 3 * 60 * 1000)
  public void checkUpdate() {
    try {
      LOGGER.info("Checking for a new manager version");
      updateChecker.fetchNewVersion();
    } catch (Exception ex) {
      LOGGER.warn("Could not get update information - {}", ex.getMessage());
    }

    packageManager.updateCacheDB();
  }

}

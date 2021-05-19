package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.manager.standalone.config.ManagerHomeCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class ManagerHomeCleanerConfiguration {

  @Autowired
  private ManagerHomeCleaner cleaner;

  @Scheduled(fixedRate = 6*60*60*1000, initialDelay = 30*1000)
  public void runCleaner() {
    cleaner.clean();
  }
}

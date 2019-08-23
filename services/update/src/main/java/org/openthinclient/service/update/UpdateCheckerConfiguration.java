package org.openthinclient.service.update;

import org.openthinclient.pkgmgr.PackageManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class UpdateCheckerConfiguration {

  @Autowired
  private UpdateChecker updateChecker;
  @Autowired
  private PackageManager packageManager;

  @Bean
  public UpdateChecker updateChecker() {
    return new UpdateChecker();
  }

  @Bean
  public UpdateRunner updateRunner() {
    return new UpdateRunner();
  }

  @Scheduled(fixedRate = 12 * 60 * 60 * 1000, initialDelay = 3 * 60 * 1000)
  public void checkUpdate() {
    updateChecker.fetchNewVersion();
    packageManager.updateCacheDB();
  }

}

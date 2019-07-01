package org.openthinclient.service.common.license;

import org.openthinclient.service.common.home.ManagerHome;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@Import({LicenseManagerConfiguration.class})
public class LicenseUpdaterConfiguration {
  @Autowired
  private LicenseUpdater licenseUpdater;

  @Autowired
  ManagerHome managerHome;

  @Scheduled(fixedRate = 12 * 60 * 60 * 1000, initialDelay = 3000)
  public void updateLicense() {
    licenseUpdater.updateLicense(managerHome.getMetadata().getServerID());
  }
}

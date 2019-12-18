package org.openthinclient.service.common.license;

import org.openthinclient.service.common.home.ManagerHome;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@DependsOn("liquibase")
@EnableJpaRepositories(basePackageClasses = {LicenseRepository.class, LicenseErrorRepository.class, Jsr310JpaConverters.class})
@EntityScan(basePackageClasses = {EncryptedLicense.class, LicenseError.class})
public class LicenseManagerConfiguration {

  @Bean
  public LicenseManager licenseManager() {
    return new LicenseManager();
  }

  @Bean
  public LicenseUpdater licenseUpdater() {
    return new LicenseUpdater();
  }
}

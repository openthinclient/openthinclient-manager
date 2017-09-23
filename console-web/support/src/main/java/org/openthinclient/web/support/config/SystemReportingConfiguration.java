package org.openthinclient.web.support.config;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.support.PackageManagerReportContributor;
import org.openthinclient.web.support.SystemReportGenerator;
import org.openthinclient.web.support.SystemReportPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemReportingConfiguration {

  @Autowired
  ManagerHome managerHome;
  @Autowired
  PackageManager packageManager;

  @Bean
  public SystemReportGenerator systemReportGenerator() {
    final SystemReportGenerator generator = new SystemReportGenerator(managerHome);

    generator.getContributors().add(new PackageManagerReportContributor(packageManager));

    return generator;
  }

  @Bean
  public SystemReportPublisher systemReportPublisher() {
    return new SystemReportPublisher(managerHome);
  }

}

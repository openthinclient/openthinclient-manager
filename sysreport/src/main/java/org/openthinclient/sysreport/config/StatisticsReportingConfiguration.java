package org.openthinclient.sysreport.config;

import org.openthinclient.common.model.service.ApplicationGroupService;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientGroupService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.sysreport.StatisticsReportPublisher;
import org.openthinclient.sysreport.generate.StatisticsReportGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class StatisticsReportingConfiguration {

  public static final String CRON_EXPRESSION = "0 32 7 * * FRI";
  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private PackageManager packageManager;
  @Autowired
  private ClientService clientService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ClientGroupService clientGroupService;

  @Bean
  public StatisticsReportGenerator statisticsReportGenerator() {
    return new StatisticsReportGenerator(managerHome, packageManager, clientService, applicationService, applicationGroupService, clientGroupService);
  }

  @Bean
  public StatisticsReportPublisher.Uploader uploader() {
    return new StatisticsReportPublisher.Uploader(managerHome.getConfiguration(PackageManagerConfiguration.class).getProxyConfiguration());
  }

  @Bean
  public StatisticsReportPublisher statisticsReportPublisher() {
    return new StatisticsReportPublisher(statisticsReportGenerator(), uploader());
  }

  // once every Friday
    @Scheduled(cron= CRON_EXPRESSION)
  // once every minute for testing
//  @Scheduled(cron = "0 * * * * *")
  public void transmitStatisticsReport() throws Exception {
    statisticsReportPublisher().publish();
  }

}

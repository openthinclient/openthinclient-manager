package org.openthinclient.sysreport.config;

import org.openthinclient.common.model.service.ApplicationGroupService;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientGroupService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.UnrecognizedClientService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.sysreport.StatisticsReportPublisher;
import org.openthinclient.sysreport.generate.StatisticsReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class StatisticsReportingConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsReportingConfiguration.class);

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private PackageManager packageManager;
  @Autowired
  private ClientService clientService;
  @Autowired
  private UnrecognizedClientService unrecognizedClientService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ClientGroupService clientGroupService;

  @Bean
  public StatisticsReportGenerator statisticsReportGenerator() {
    return new StatisticsReportGenerator(managerHome, packageManager, clientService, unrecognizedClientService, applicationService, applicationGroupService, clientGroupService);
  }

  @Bean
  public StatisticsReportPublisher.Uploader uploader() {
    return new StatisticsReportPublisher.Uploader(managerHome.getConfiguration(PackageManagerConfiguration.class).getProxyConfiguration());
  }

  @Bean
  public StatisticsReportPublisher statisticsReportPublisher() {
    return new StatisticsReportPublisher(statisticsReportGenerator(), uploader());
  }


  @Scheduled(cron = "0 30 7 * * FRI") // Friday morning
  @Scheduled(cron = "0 0 12 * * WED") // Wednesday noon
  // @Scheduled(cron = "0 * * * * *")    // every minute for testing
  public void transmitStatisticsReport() throws Exception {
    if(managerHome.getMetadata().isUsageStatisticsEnabled())
      statisticsReportPublisher().publish();
    else
      LOGGER.debug("Statistics transmission has been disabled. Skipping.");
  }

}

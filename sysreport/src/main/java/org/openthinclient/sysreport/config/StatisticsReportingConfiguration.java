package org.openthinclient.sysreport.config;

import org.openthinclient.common.model.service.*;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.license.LicenseManager;
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
  @Autowired
  private RealmService realmService;
  @Autowired
  private UserService userService;
  @Autowired
  private UserGroupService userGroupService;
  @Autowired
  private DeviceService deviceService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private PrinterService printerService;
  @Autowired
  private HardwareTypeService hardwareTypeService;
  @Autowired
  private LicenseManager licenseManager;

  @Bean
  public StatisticsReportGenerator statisticsReportGenerator() {
    return new StatisticsReportGenerator(managerHome, packageManager, clientService, applicationService, applicationGroupService, clientGroupService, realmService, userService, userGroupService, deviceService, locationService, printerService, hardwareTypeService, licenseManager);
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
    if(managerHome.getMetadata().isUsageStatisticsEnabled())
      statisticsReportPublisher().publish();
    else
      LOGGER.debug("Statistics transmission has been disabled. Skipping.");
  }

}

package org.openthinclient.sysreport.config;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.sysreport.StatisticsReport;
import org.openthinclient.sysreport.StatisticsReportPackage;
import org.openthinclient.sysreport.generate.StatisticsReportGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@EnableScheduling
public class StatisticsReportingConfiguration {

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private PackageManager packageManager;

  @Bean
  public StatisticsReportGenerator statisticsReportGenerator() {
    return new StatisticsReportGenerator(managerHome, packageManager);
  }

  // once every Friday
  //  @Scheduled(cron="0 0 0 * * 5")
  // once every minute for testing
  @Scheduled(cron = "0 * * * * *")
  public void transmitStatisticsReport() throws Exception {

    final StatisticsReport report = statisticsReportGenerator().generateReport();

    final StatisticsReportPackage reportPackage = new StatisticsReportPackage(report);
    final Path tempFile = Files.createTempFile("stats-", ".zip");
    System.out.println(tempFile);
    try (final OutputStream out = Files.newOutputStream(tempFile)) {
      reportPackage.save(out);
    }
  }

}

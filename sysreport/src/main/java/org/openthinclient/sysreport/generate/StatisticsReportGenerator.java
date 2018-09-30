package org.openthinclient.sysreport.generate;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.sysreport.StatisticsReport;

public class StatisticsReportGenerator extends AbstractReportGenerator<StatisticsReport> {
  public StatisticsReportGenerator(ManagerHome managerHome, PackageManager packageManager) {
    super(managerHome);
    contributors.add(new NetworkInterfaceContributor());
    contributors.add(new PackageManagerSummaryReportContributor(packageManager));
  }

  @Override
  protected StatisticsReport createReportInstance() {
    return new StatisticsReport();
  }
}

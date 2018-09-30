package org.openthinclient.sysreport.generate;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.sysreport.StatisticsReport;

public class PackageManagerSummaryReportContributor extends AbstractPackageManagerReportContributor<StatisticsReport> {
  public PackageManagerSummaryReportContributor(PackageManager packageManager) {
    super(packageManager);
  }

  @Override
  protected void onInstallable(StatisticsReport report, Package aPackage) {
    report.getPackageManager().getInstalled().add(new StatisticsReport.PackageSummary(aPackage.getName(), "" + aPackage.getVersion()));
  }

  @Override
  protected void onSource(StatisticsReport report, Source source) {
    report.getPackageManager().getSources().add(convert(source));
  }

}

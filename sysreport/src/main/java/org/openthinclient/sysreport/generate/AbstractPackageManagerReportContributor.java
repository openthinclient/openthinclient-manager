package org.openthinclient.sysreport.generate;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.sysreport.AbstractReport;

public abstract class AbstractPackageManagerReportContributor<T extends AbstractReport> implements ReportContributor<T> {

  protected final PackageManager packageManager;

  public AbstractPackageManagerReportContributor(PackageManager packageManager) {
    this.packageManager = packageManager;
  }

  @Override
  public void contribute(T report) {

    // FIXME this really is not the right place. DiskSpace computation should be part of some general logic
    // FIXME in addition, the free disk space is measured as kb instead of bytes.
    report.getServer().setFreeDiskSpace(packageManager.getFreeDiskSpace() * 1024);

    packageManager.getInstalledPackages() //
            .forEach(pkg -> onInstalled(report, pkg));

    packageManager.getInstallablePackages() //
            .forEach(pkg -> onInstallable(report, pkg));

    packageManager.getSourcesList() //
            .getSources() //
            .forEach(source -> onSource(report, source));


  }

  protected void onSource(T report, Source source) {
  }

  protected void onInstallable(T report, Package aPackage) {
  }

  protected void onInstalled(T report, Package aPackage) {
  }

  protected org.openthinclient.sysreport.Source convert(Source source) {
    final org.openthinclient.sysreport.Source converted = new org.openthinclient.sysreport.Source();
    converted.setEnabled(source.isEnabled());
    converted.setLastUpdated(source.getLastUpdated());
    converted.setUrl(source.getUrl());
    return converted;

  }
}

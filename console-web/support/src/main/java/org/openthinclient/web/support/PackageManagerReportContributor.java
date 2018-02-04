package org.openthinclient.web.support;

import com.google.common.base.Strings;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageInstalledContent;
import org.openthinclient.sysreport.SystemReport;

import java.util.Comparator;
import java.util.stream.Collectors;

public class PackageManagerReportContributor implements SystemReportGenerator.ReportContributor {

  private final PackageManager packageManager;

  public PackageManagerReportContributor(PackageManager packageManager) {
    this.packageManager = packageManager;
  }

  @Override
  public void contribute(SystemReport report) {

    // FIXME this really is not the right place. DiskSpace computation should be part of some general logic
    // FIXME in addition, the free disk space is measured as kb instead of bytes.
    report.getServer().setFreeDiskSpace(packageManager.getFreeDiskSpace() * 1024);

    final PackageManagerConfiguration config = packageManager.getConfiguration();

    final SystemReport.Network network = report.getNetwork();

    if (config.getProxyConfiguration() != null) {
      if (!Strings.isNullOrEmpty(config.getProxyConfiguration().getPassword()))
        network.getProxy().setPasswordSpecified(true);

      network.getProxy().setEnabled(config.getProxyConfiguration().isEnabled());
      network.getProxy().setHost(config.getProxyConfiguration().getHost());
      network.getProxy().setPort(config.getProxyConfiguration().getPort());
      network.getProxy().setUser(config.getProxyConfiguration().getUser());
    }


    packageManager.getInstalledPackages() //
            .stream() //
            .map(this::convert) //
            .forEach(report.getPackageManager().getInstalled()::add);

    packageManager.getInstallablePackages() //
            .stream() //
            .map(this::convert) //
            .forEach(report.getPackageManager().getInstallable()::add);

  }

  protected org.openthinclient.sysreport.Package convert(Package pkg) {

    final org.openthinclient.sysreport.Package converted = new org.openthinclient.sysreport.Package();

    converted.setId(pkg.getId());
    converted.setSource(pkg.getSource().getUrl().toString());
    converted.setInstalledSize(pkg.getInstalledSize());
    converted.setVersion(pkg.getVersion().toString());
    converted.setArchitecture(pkg.getArchitecture());
    converted.setDistribution(pkg.getDistribution());
    converted.setName(pkg.getName());
    converted.setPriority(pkg.getPriority());
    converted.setSize(pkg.getSize());
    converted.setInstalled(pkg.isInstalled());

    if (pkg.isInstalled()) {
      converted.setInstalledContents(
              packageManager.getInstalledPackageContents(pkg).stream() //
                      .map(this::convert) //
                      .sorted(Comparator.comparingLong(org.openthinclient.sysreport.PackageInstalledContent::getSequence)) //
                      .collect(Collectors.toList())
      );
    }

    return converted;

  }

  private org.openthinclient.sysreport.PackageInstalledContent convert(PackageInstalledContent installedPackageContent) {
    final org.openthinclient.sysreport.PackageInstalledContent converted = new org.openthinclient.sysreport.PackageInstalledContent();
    converted.setId(installedPackageContent.getId());
    converted.setPath(installedPackageContent.getPath());
    converted.setSequence(installedPackageContent.getSequence());
    converted.setSha1(installedPackageContent.getSha1());

    switch (installedPackageContent.getType()) {
      case FILE:
        converted.setType(org.openthinclient.sysreport.PackageInstalledContent.Type.FILE);
        break;
      case DIR:
        converted.setType(org.openthinclient.sysreport.PackageInstalledContent.Type.DIR);
        break;
      case SYMLINK:
        converted.setType(org.openthinclient.sysreport.PackageInstalledContent.Type.SYMLINK);
        break;
    }
    return converted;
  }

}

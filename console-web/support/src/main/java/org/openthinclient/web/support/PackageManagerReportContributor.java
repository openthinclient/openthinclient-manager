package org.openthinclient.web.support;

import com.google.common.base.Strings;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;

public class PackageManagerReportContributor implements SystemReportGenerator.ReportContributor {

  private final PackageManager packageManager;

  public PackageManagerReportContributor(PackageManager packageManager) {
    this.packageManager = packageManager;
  }

  @Override
  public void contribute(SystemReport report) {

    // FIXME this really is not the right place. DiskSpace computation should be part of some general logic
    report.getServer().setFreeDiskSpace(packageManager.getFreeDiskSpace());

    // creating a clone as we're going to change the configuration to avoid transmitting passwords.
    final PackageManagerConfiguration cloned = packageManager.getConfiguration().clone();

    if (cloned.getProxyConfiguration() != null && !Strings.isNullOrEmpty(cloned.getProxyConfiguration().getPassword()))
      cloned.getProxyConfiguration().setPassword("---- deleted ----");
    final PackageManagerSubsystem subsystem = new PackageManagerSubsystem();
    subsystem.setConfiguration(cloned);

    report.addSubsystem(subsystem);
  }

  public static final class PackageManagerSubsystem extends SystemReport.Subsystem {
    private PackageManagerConfiguration configuration;

    public PackageManagerSubsystem() {
      super("package-manager");
    }

    public PackageManagerConfiguration getConfiguration() {
      return configuration;
    }

    public void setConfiguration(PackageManagerConfiguration configuration) {
      this.configuration = configuration;
    }
  }
}

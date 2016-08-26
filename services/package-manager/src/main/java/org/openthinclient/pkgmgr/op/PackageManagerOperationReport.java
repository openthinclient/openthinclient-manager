package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.db.Package;

import java.util.ArrayList;
import java.util.List;

public class PackageManagerOperationReport {

  private final List<PackageReport> packageReports;

  public PackageManagerOperationReport() {
    packageReports = new ArrayList<>();
  }

  public List<PackageReport> getPackageReports() {
    return packageReports;
  }

  public enum PackageReportType {
    INSTALL,
    UNINSTALL,
    UPGRADE,
    DOWNGRADE
  }

  public static class PackageReport {
    private final Package pkg;
    private final PackageReportType type;

    public PackageReport(Package pkg, PackageReportType type) {
      this.pkg = pkg;
      this.type = type;
    }

    public Package getPackage() {
      return pkg;
    }

    public PackageReportType getType() {
      return type;
    }
  }
  
  public void addPackageReport(PackageReport packageReport) {
    packageReports.add(packageReport);
  }
}

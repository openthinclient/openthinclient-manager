package org.openthinclient.pkgmgr.op;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.openthinclient.pkgmgr.db.Package;

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
    DOWNGRADE, 
    FAIL
  }

  public static class PackageReport {
    private final Package pkg;
    private final PackageReportType type;

    public PackageReport(Package pkg, PackageReportType type) {
      this.pkg = pkg;
      this.type = type;
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this)
          .append("pkg", pkg)
          .append("type", type)
          .toString();
    }
    
    public Package getPackage() {
      return pkg;
    }

    public PackageReportType getType() {
      return type;
    }
    
    public String getPackageName() {
      return pkg.getName();
    }
  }
  
  public void addPackageReport(PackageReport packageReport) {
    packageReports.add(packageReport);
  }
}

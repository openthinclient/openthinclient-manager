package org.openthinclient.web.pkgmngr.event;

import java.util.List;

import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport.PackageReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport.PackageReportType;
import org.springframework.context.ApplicationEvent;

public class PackageEvent extends ApplicationEvent {
  public PackageEvent(PackageManagerOperationReport report) {
    super(report);
  }

  public List<PackageManagerOperationReport.PackageReport> getReports() {
    return ((PackageManagerOperationReport) getSource()).getPackageReports();
  }

  /**
   * Whether any packages actually changed (i.e. not all operations failed)
   */
  public boolean changesOccured() {
    for(PackageReport report: getReports()) {
      PackageReportType type = report.getType();
      if(!type.equals(PackageReportType.FAIL)
           && !type.equals(PackageReportType.DOWNLOAD)) {
        return true;
      }
    }
    return false;
  }
}

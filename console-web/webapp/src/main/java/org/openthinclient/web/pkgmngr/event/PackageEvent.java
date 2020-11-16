package org.openthinclient.web.pkgmngr.event;

import java.util.List;

import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.springframework.context.ApplicationEvent;

public class PackageEvent extends ApplicationEvent {
  public PackageEvent(PackageManagerOperationReport report) {
    super(report);
  }

  public List<PackageManagerOperationReport.PackageReport> getReports() {
    return ((PackageManagerOperationReport) getSource()).getPackageReports();
  }
}

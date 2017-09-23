package org.openthinclient.web.support;

import org.openthinclient.service.common.home.ManagerHome;

import java.util.ArrayList;
import java.util.List;

public class SystemReportGenerator {

  private final ManagerHome managerHome;
  private final List<ReportContributor> contributors;

  public SystemReportGenerator(ManagerHome managerHome) {
    this.managerHome = managerHome;
    contributors = new ArrayList<>();
  }

  public List<ReportContributor> getContributors() {
    return contributors;
  }

  public SystemReport generateReport() {

    final SystemReport report = new SystemReport();

    report.getServer().setServerId(managerHome.getMetadata().getServerID());

    for (ReportContributor contributor : contributors) {
      contributor.contribute(report);
    }

    return report;
  }

  public interface ReportContributor {
    void contribute(SystemReport report);
  }
}

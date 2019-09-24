package org.openthinclient.sysreport.generate;

import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.sysreport.AbstractReport;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractReportGenerator<T extends AbstractReport> {
  protected final List<ReportContributor<? super T>> contributors;

  public AbstractReportGenerator(ManagerHome managerHome) {
    contributors = new ArrayList<>();
    contributors.add(new ServerReportContributor(managerHome));
    contributors.add(new ManagerReportContributor());
  }

  public List<ReportContributor<? super T>> getContributors() {
    return contributors;
  }

  protected abstract T createReportInstance();

  public T generateReport() {

    final T report = createReportInstance();

    for (ReportContributor<? super T> contributor : contributors) {
      contributor.contribute(report);
    }

    return report;
  }

}

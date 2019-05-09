package org.openthinclient.sysreport.generate;

import org.openthinclient.sysreport.AbstractReport;

public interface ReportContributor<T extends AbstractReport> {
  void contribute(T report);
}

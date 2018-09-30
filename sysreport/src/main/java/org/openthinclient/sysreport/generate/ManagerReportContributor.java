package org.openthinclient.sysreport.generate;

import org.openthinclient.sysreport.AbstractReport;

import java.util.HashMap;
import java.util.Map;

public class ManagerReportContributor implements ReportContributor<AbstractReport> {
  @Override
  public void contribute(AbstractReport report) {
    final Map<String, String> properties = new HashMap<>();
    report.getManager().getJava().setProperties(properties);

    System.getProperties().forEach((key, value) -> {
      properties.put(""+key, ""+value);
    });

  }
}

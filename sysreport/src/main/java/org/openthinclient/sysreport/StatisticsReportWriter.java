package org.openthinclient.sysreport;

import java.io.IOException;
import java.io.OutputStream;

public class StatisticsReportWriter extends AbstractReportWriter<StatisticsReport> {

  public StatisticsReportWriter(StatisticsReport report) {
    super(report);
  }

  public void write(OutputStream out) throws IOException {
    writeReport(out);
  }

}

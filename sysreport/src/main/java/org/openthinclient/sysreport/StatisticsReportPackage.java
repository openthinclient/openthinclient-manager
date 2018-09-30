package org.openthinclient.sysreport;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.IOException;
import java.time.Instant;

public class StatisticsReportPackage extends AbstractReportPackage<StatisticsReport> {
  public StatisticsReportPackage(StatisticsReport report) {
    super(report, "statistics-report.json");
  }

  @Override
  protected void writeAdditionalContents(ZipArchiveOutputStream zip, Instant creationTimestamp) throws IOException {

  }
}

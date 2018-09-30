package org.openthinclient.sysreport;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A package containing the {@link SystemReport} and various logfiles. The package is a ZIP file
 * with a defined structure.
 */
public class SystemReportPackage extends AbstractReportPackage<SystemReport> {

  public static final String PATH_REPORTS = "tcos-reports/";
  private static final String PATH_LOGS = "logs/";
  /**
   * A {@link Map} containing references to report files, by client MAC address. The key in the
   * {@link Map} is the MAC address for a specific client.
   */
  private final Map<String, List<Path>> tcosReports;
  private final List<Path> logfiles;

  public SystemReportPackage(SystemReport report) {
    super(report, "system-report.json");
    tcosReports = new HashMap<>();
    logfiles = new ArrayList<>();
  }

  /**
   * Append a tcos report file for the specified macAddress.
   *
   * @param macAddress the macAddress of the client
   * @param reportFile a {@link Path} pointing to a tcos report file, which shall be included in the report package.
   */
  public void addReport(String macAddress, Path reportFile) {

    if (!Files.isRegularFile(reportFile))
      throw new IllegalArgumentException("Provided path does not point to a valid file: " + reportFile);

    final List<Path> paths = tcosReports.computeIfAbsent(macAddress, k -> new ArrayList<>());
    paths.add(reportFile);
  }

  public void addLogfile(Path logfile) {
    if (!Files.isRegularFile(logfile))
      throw new IllegalArgumentException("Provided path does not point to a valid file: " + logfile);

    logfiles.add(logfile);
  }

  @Override
  protected void writeAdditionalContents(ZipArchiveOutputStream zip, Instant creationTimestamp) throws IOException {
    writeTcosReports(zip, creationTimestamp);

    writeLogfiles(zip, creationTimestamp);
  }

  private void writeLogfiles(ZipArchiveOutputStream zip, Instant creationTimestamp) throws IOException {
    zip.putArchiveEntry(new ZipArchiveEntry(PATH_LOGS));
    zip.closeArchiveEntry();

    for (Path logfile : logfiles) {
      writeLogfile(zip, logfile, creationTimestamp);
    }
  }

  private void writeLogfile(ZipArchiveOutputStream zip, Path logfile, Instant creationTimestamp) throws IOException {
    final ZipArchiveEntry entry = new ZipArchiveEntry(PATH_LOGS + logfile.getFileName().toString());
    entry.setCreationTime(Files.getLastModifiedTime(logfile));
    zip.putArchiveEntry(entry);
    Files.copy(logfile, zip);
    zip.closeArchiveEntry();
  }

  private void writeTcosReports(ZipArchiveOutputStream zip, Instant creationTimestamp) throws IOException {
    zip.putArchiveEntry(new ZipArchiveEntry(PATH_REPORTS));
    zip.closeArchiveEntry();

    for (Map.Entry<String, List<Path>> e : tcosReports.entrySet()) {
      writeTcosReports(zip, e.getKey(), e.getValue(), creationTimestamp);
    }
  }

  private void writeTcosReports(ZipArchiveOutputStream zip, String clientMacAddress, List<Path> tcosReports, Instant creationTimestamp) throws IOException {
    zip.putArchiveEntry(new ZipArchiveEntry(PATH_REPORTS + clientMacAddress+"/"));
    zip.closeArchiveEntry();

    for (Path tcosReport : tcosReports) {
      writeTcosReport(zip, clientMacAddress, tcosReport, creationTimestamp);
    }
  }

  private void writeTcosReport(ZipArchiveOutputStream zip, String clientMacAddress, Path tcosReport, Instant creationTimestamp) throws IOException {
    final ZipArchiveEntry entry = new ZipArchiveEntry(PATH_REPORTS + clientMacAddress + "/" + tcosReport.getFileName().toString());
    entry.setCreationTime(Files.getLastModifiedTime(tcosReport));
    zip.putArchiveEntry(entry);
    Files.copy(tcosReport, zip);
    zip.closeArchiveEntry();
  }


}

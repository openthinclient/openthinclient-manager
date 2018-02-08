package org.openthinclient.sysreport;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A package containing the {@link SystemReport} and various logfiles. The package is a ZIP file
 * with a defined structure.
 */
public class SystemReportPackage {

  public static final String PATH_REPORTS = "tcos-reports/";
  private static final String PATH_LOGS = "logs/";
  private final SystemReport report;
  /**
   * A {@link Map} containing references to report files, by client MAC address. The key in the
   * {@link Map} is the MAC address for a specific client.
   */
  private final Map<String, List<Path>> tcosReports;
  private final List<Path> logfiles;

  public SystemReportPackage(SystemReport report) {
    this.report = report;
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

  public void save(OutputStream out) throws IOException {

    try (final ZipOutputStream zip = new ZipOutputStream(out)) {

      final Instant creationTimestamp = Instant.now();
      // we're ok if the compression actually takes a while. Save some space during transmission
      zip.setLevel(9);
      zip.setComment("openthinclient manager system report. Generated: " + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(creationTimestamp));

      writeSystemReport(zip, creationTimestamp);

      writeTcosReports(zip, creationTimestamp);

      writeLogfiles(zip, creationTimestamp);

      zip.finish();
    }

  }

  private void writeLogfiles(ZipOutputStream zip, Instant creationTimestamp) throws IOException {
    zip.putNextEntry(new ZipEntry(PATH_LOGS));
    zip.closeEntry();

    for (Path logfile : logfiles) {
      writeLogfile(zip, logfile, creationTimestamp);
    }
  }

  private void writeLogfile(ZipOutputStream zip, Path logfile, Instant creationTimestamp) throws IOException {
    final ZipEntry entry = new ZipEntry(PATH_LOGS + logfile.getFileName().toString());
    entry.setCreationTime(Files.getLastModifiedTime(logfile));
    zip.putNextEntry(entry);
    Files.copy(logfile, zip);
    zip.closeEntry();
  }

  private void writeTcosReports(ZipOutputStream zip, Instant creationTimestamp) throws IOException {
    zip.putNextEntry(new ZipEntry(PATH_REPORTS));
    zip.closeEntry();

    for (Map.Entry<String, List<Path>> e : tcosReports.entrySet()) {
      writeTcosReports(zip, e.getKey(), e.getValue(), creationTimestamp);
    }
  }

  private void writeTcosReports(ZipOutputStream zip, String clientMacAddress, List<Path> tcosReports, Instant creationTimestamp) throws IOException {
    zip.putNextEntry(new ZipEntry(PATH_REPORTS + clientMacAddress+"/"));
    zip.closeEntry();

    for (Path tcosReport : tcosReports) {
      writeTcosReport(zip, clientMacAddress, tcosReport, creationTimestamp);
    }
  }

  private void writeTcosReport(ZipOutputStream zip, String clientMacAddress, Path tcosReport, Instant creationTimestamp) throws IOException {
    final ZipEntry entry = new ZipEntry(PATH_REPORTS + clientMacAddress + "/" + tcosReport.getFileName().toString());
    entry.setCreationTime(Files.getLastModifiedTime(tcosReport));
    zip.putNextEntry(entry);
    Files.copy(tcosReport, zip);
    zip.closeEntry();
  }

  private void writeSystemReport(ZipOutputStream zip, Instant creationTimestamp) throws IOException {
    final ZipEntry entry = new ZipEntry("system-report.json");
    entry.setCreationTime(FileTime.from(creationTimestamp));
    zip.putNextEntry(entry);

    final ObjectMapper mapper = new ObjectMapper();
    // prevent the ObjectWriter to close the stream
    mapper.disable(SerializationFeature.CLOSE_CLOSEABLE)
    .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    final ObjectWriter writer = mapper.writer();

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    writer.writeValue(baos, report);

    zip.write(baos.toByteArray());

    zip.flush();
    zip.closeEntry();
  }


}

package org.openthinclient.sysreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

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
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A package containing the {@link SystemReport} and various logfiles. The package is a ZIP file
 * with a defined structure.
 */
public class SystemReportPackage {

  public static final String PATH_REPORTS = "tcos-reports/";
  private final SystemReport report;
  /**
   * A {@link Map} containing references to report files, by client MAC address. The key in the
   * {@link Map} is the MAC address for a specific client.
   */
  private final Map<String, List<Path>> tcosReports;

  public SystemReportPackage(SystemReport report) {
    this.report = report;
    tcosReports = new HashMap<>();
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


  public void save(Supplier<OutputStream> outputSupplier) throws IOException {

    try (final OutputStream out = outputSupplier.get(); final ZipOutputStream zip = new ZipOutputStream(out)) {

      final Instant creationTimestamp = Instant.now();
      // we're ok if the compression actually takes a while. Save some space during transmission
      zip.setLevel(9);
      zip.setComment("openthinclient manager system report. Generated: " + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(creationTimestamp));

      writeSystemReport(zip, creationTimestamp);

      writeTcosReports(zip, creationTimestamp);


    }

  }

  private void writeTcosReports(ZipOutputStream zip, Instant creationTimestamp) throws IOException {
    zip.putNextEntry(new ZipEntry(PATH_REPORTS));

    for (Map.Entry<String, List<Path>> e : tcosReports.entrySet()) {
      writeTcosReports(zip, e.getKey(), e.getValue(), creationTimestamp);
    }
  }

  private void writeTcosReports(ZipOutputStream zip, String clientMacAddress, List<Path> tcosReports, Instant creationTimestamp) throws IOException {

    zip.putNextEntry(new ZipEntry(PATH_REPORTS + clientMacAddress+"/"));

    for (Path tcosReport : tcosReports) {
      writeTcosReport(zip, clientMacAddress, tcosReport, creationTimestamp);
    }
  }

  private void writeTcosReport(ZipOutputStream zip, String clientMacAddress, Path tcosReport, Instant creationTimestamp) throws IOException {
    zip.putNextEntry(new ZipEntry(PATH_REPORTS + clientMacAddress + "/" + tcosReport.getFileName().toString()));
    Files.copy(tcosReport, zip);
  }

  private void writeSystemReport(ZipOutputStream zip, Instant creationTimestamp) throws IOException {

    final ZipEntry entry = new ZipEntry("system-report.json");
    entry.setComment("Main report file");
    entry.setCreationTime(FileTime.from(creationTimestamp));

    final ObjectMapper mapper = new ObjectMapper();
    // prevent the ObjectWriter to close the stream
    mapper.disable(SerializationFeature.CLOSE_CLOSEABLE);
    final ObjectWriter writer = mapper.writer();

    writer.writeValue(zip, report);

  }


}

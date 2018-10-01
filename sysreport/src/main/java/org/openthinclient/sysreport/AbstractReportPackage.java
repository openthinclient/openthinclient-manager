package org.openthinclient.sysreport;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public abstract class AbstractReportPackage<T extends AbstractReport> {
  protected final T report;
  private final String reportFilename;

  public AbstractReportPackage(T report, String reportFilename) {
    this.reportFilename = reportFilename;
    this.report = report;
  }

  public void save(OutputStream out) throws IOException {

    try (final ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out)) {

      final Instant creationTimestamp = Instant.now();
      // we're ok if the compression actually takes a while. Save some space during transmission
      zip.setLevel(9);
      zip.setComment("openthinclient manager system report. Generated: " + DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).format(creationTimestamp));

      writeReportReport(zip, creationTimestamp);

      writeAdditionalContents(zip, creationTimestamp);

      zip.finish();
    }

  }

  protected abstract void writeAdditionalContents(ZipArchiveOutputStream zip, Instant creationTimestamp) throws IOException;

  private void writeReportReport(ZipArchiveOutputStream zip, Instant creationTimestamp) throws IOException {
    final ZipArchiveEntry entry = new ZipArchiveEntry(reportFilename);
    entry.setCreationTime(FileTime.from(creationTimestamp));
    zip.putArchiveEntry(entry);

    final ObjectMapper mapper = new ObjectMapper();
    // prevent the ObjectWriter to close the stream
    mapper.disable(SerializationFeature.CLOSE_CLOSEABLE)
            .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
            // used to serialize java.time-Classes
            .registerModule(new JavaTimeModule());
    final ObjectWriter writer = mapper.writer();

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    writer.writeValue(baos, report);

    zip.write(baos.toByteArray());

    zip.closeArchiveEntry();
  }
}

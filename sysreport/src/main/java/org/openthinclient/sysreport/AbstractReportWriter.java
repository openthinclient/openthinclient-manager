package org.openthinclient.sysreport;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractReportWriter<T extends AbstractReport> {
  private final T report;

  public AbstractReportWriter(T report) {
    this.report = report;
  }

  protected void writeReport(OutputStream out) throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    // prevent the ObjectWriter to close the stream
    mapper.disable(SerializationFeature.CLOSE_CLOSEABLE)
            .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
            // used to serialize java.time-Classes
            .registerModule(new JavaTimeModule());
    final ObjectWriter writer = mapper.writer();

    writer.writeValue(out, report);
  }
}

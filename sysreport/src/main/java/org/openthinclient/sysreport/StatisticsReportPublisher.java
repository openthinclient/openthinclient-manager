package org.openthinclient.sysreport;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.manager.util.http.impl.AbstractHttpAccessorBase;
import org.openthinclient.sysreport.generate.StatisticsReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class StatisticsReportPublisher {

  private final Logger LOGGER = LoggerFactory.getLogger(StatisticsReportPublisher.class);

  private final Uploader uploader;
  private final StatisticsReportGenerator statisticsReportGenerator;

  public StatisticsReportPublisher(StatisticsReportGenerator statisticsReportGenerator, Uploader uploader) {
    this.uploader = uploader;
    this.statisticsReportGenerator = statisticsReportGenerator;
  }

  public void publish() throws Exception {

    final StatisticsReport report = statisticsReportGenerator.generateReport();

    final StatisticsReportWriter statisticsWriter = new StatisticsReportWriter(report);
    final Path tempFile = Files.createTempFile("stats-", ".json");
    LOGGER.debug("Writing statistics report file: {}", tempFile);
    try (final OutputStream out = Files.newOutputStream(tempFile)) {
      statisticsWriter.write(out);
    }

    uploader.upload(tempFile);
    LOGGER.debug("Upload completed.");
    Files.delete(tempFile);
  }

  public static class Uploader extends AbstractHttpAccessorBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(Uploader.class);

    private static final URI BASE_URI = URI.create("https://statistics.openthinclient.com/v1/statistics");

    public Uploader(NetworkConfiguration.ProxyConfiguration proxyConfig) {
      super(proxyConfig, "openthinclient.org manager");
    }

    public void upload(Path statisticsFile) {

      final HttpPost put = new HttpPost(BASE_URI);
      put.setEntity(new FileEntity(statisticsFile.toFile(), ContentType.APPLICATION_JSON));

      final HttpResponse response;
      try {
        response = httpClient.execute(put);
      } catch (IOException e) {
        LOGGER.error("Statistics Report upload failed", e);
        return;
      }

      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode < 200 || statusCode > 299) {
        LOGGER.error("System report upload failed.", response);
      }
    }


  }
}

package org.openthinclient.web.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.manager.util.http.impl.AbstractHttpAccessorBase;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.sysreport.SystemReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

public class SystemReportPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(SystemReportPublisher.class);

  private static final char[] ID_CHARS = "abcdefghkmnorstuvwxyz23456789".toCharArray();
  private static final int ID_LENGTH = 6;
  private final ManagerHome managerHome;
  private final Uploader uploader;

  public SystemReportPublisher(ManagerHome managerHome) {
    this.managerHome = managerHome;
    this.uploader = new Uploader(managerHome.getConfiguration(PackageManagerConfiguration.class).getProxyConfiguration());
  }

  public static String createSupportId() {

    final SecureRandom random = new SecureRandom();

    char[] chars = new char[ID_LENGTH];

    for (int i = 0; i < chars.length; i++) {
      chars[i] = ID_CHARS[random.nextInt(chars.length)];
    }

    return new String(chars);
  }

  private String createReportFileName(String supportId) {
    return "sysreport-" + managerHome.getMetadata().getServerID() + "__" + supportId + ".json";
  }

  public SystemReportUploadResult upload(SystemReport report) {

    final String supportId = createSupportId();

    final Path tempFile;
    try {
      tempFile = serializeToTempFile(report);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write system report", e);
    }

    final String serverId = managerHome.getMetadata().getServerID();
    uploader.upload(tempFile, serverId, supportId);

    return new SystemReportUploadResult(serverId, supportId);
  }

  private Path serializeToTempFile(SystemReport report) throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final ObjectWriter writer = mapper.writer();

    final Path tempFile = Files.createTempFile("system-report-", ".json");

    LOGGER.info("Storing system report to temporary file {}", tempFile);

    try (final OutputStream out = Files.newOutputStream(tempFile)) {
      writer.writeValue(out, report);
    }
    return tempFile;
  }

  public static final class SystemReportUploadResult {
    private final String serverId;
    private final String supportId;

    public SystemReportUploadResult(String serverId, String supportId) {
      this.serverId = serverId;
      this.supportId = supportId;
    }

    public String getServerId() {
      return serverId;
    }

    public String getSupportId() {
      return supportId;
    }

    @Override
    public String toString() {
      return "SystemReportUploadResult{" +
              "serverId='" + serverId + '\'' +
              ", supportId='" + supportId + '\'' +
              '}';
    }
  }

  public static class Uploader extends AbstractHttpAccessorBase {
    private static final URI BASE_URI = URI.create("https://uht94fkwy5.execute-api.eu-central-1.amazonaws.com/production/system-report");

    public Uploader(NetworkConfiguration.ProxyConfiguration proxyConfig) {
      super(proxyConfig);
    }

    public void upload(Path reportFile, String serverId, String supportId) {

      final HttpPost put = new HttpPost(BASE_URI);
      put.setHeader("x-otc-server-id", serverId);
      put.setHeader("x-otc-support-id", supportId);
      put.setEntity(new FileEntity(reportFile.toFile(), ContentType.APPLICATION_JSON));

      final HttpResponse response;
      try {
        response = httpClient.execute(put);
      } catch (IOException e) {
        throw new RuntimeException("Report upload failed", e);
      }

      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode < 200 || statusCode > 299) {
        LOGGER.error("System report upload failed.", response);
        throw new RuntimeException("Report upload failed. HTTP Status: " + statusCode + ". " + response.getStatusLine().getReasonPhrase());
      }
    }


  }
}

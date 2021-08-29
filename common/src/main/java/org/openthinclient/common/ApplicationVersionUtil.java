package org.openthinclient.common;

import org.openthinclient.DownloadManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationVersionUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationVersionUtil.class);

  public static PomProperties readPomProperties() {
    InputStream inputStream = DownloadManagerFactory.class.getResourceAsStream("/META-INF/maven/org.openthinclient/manager-common/pom.properties");
    PomProperties pom = new PomProperties();
    try {
      List<String> content = read(inputStream);
      if (content.size() > 1) {
        pom.setBuildDate(content.get(1).substring(1));
      }
      if (content.size() > 2) {
        pom.setVersion(content.get(2).substring(8));
      }
    } catch (Exception e) {
      LOGGER.error("Cannot read build-date from pom.properties.");
    }
    return pom;
  }

  public static List<String> read(InputStream input) throws IOException {
    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
      return buffer.lines().collect(Collectors.toList());
    }
  }

  public static class PomProperties {

    private String version;
    private String buildDate;

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public String getBuildDate() {
      return buildDate;
    }

    public void setBuildDate(String buildDate) {
      this.buildDate = buildDate;
    }
  }

}

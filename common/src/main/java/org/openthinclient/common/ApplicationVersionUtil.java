package org.openthinclient.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.openthinclient.DownloadManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ApplicationVersionUtil
 */
public class ApplicationVersionUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationVersionUtil.class);

  public static String readVersionFromPomProperties() {
    InputStream inputStream = DownloadManagerFactory.class.getResourceAsStream("/META-INF/maven/org.openthinclient/manager-common/pom.properties");
    Properties p = new Properties();
    String version = null;
    try {
      p.load(inputStream);
      version = p.getProperty("version");
    } catch (Exception e) {
      LOGGER.error("Cannot read version from pom.properties.", e);
    }
    return version;
  }

  public static String readBuildDateFromPomProperties() {
    InputStream inputStream = DownloadManagerFactory.class.getResourceAsStream("/META-INF/maven/org.openthinclient/manager-common/pom.properties");
    try {
      List<String> content = read(inputStream);
      if (content.size() > 1) {
        return content.get(1).substring(1);
      }
    } catch (Exception e) {
      LOGGER.error("Cannot read build-date from pom.properties.");
    }
    return null;
  }

  public static List<String> read(InputStream input) throws IOException {
    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
      return buffer.lines().collect(Collectors.toList());
    }
  }

}

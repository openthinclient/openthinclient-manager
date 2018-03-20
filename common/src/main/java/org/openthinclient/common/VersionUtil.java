package org.openthinclient.common;

import java.io.InputStream;
import java.util.Properties;
import org.openthinclient.DownloadManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VersionUtil
 */
public class VersionUtil {

  private static final Logger logger = LoggerFactory.getLogger(VersionUtil.class);

  public static String readApplicationVersion() {

    InputStream inputStream = DownloadManagerFactory.class.getResourceAsStream("/META-INF/maven/org.openthinclient/manager-common/pom.properties");
    Properties p = new Properties();
    String version = null;
    if (inputStream != null) {
      try {
        p.load(inputStream);
        version = p.getProperty("version");
      } catch (Exception e) {
        logger.error("Cannot read version from pom.properties.", e);
      }
    } else {
      logger.error("Cannot find pom.properties, userAgent.version is unset!");
    }
    return version;
  }

}

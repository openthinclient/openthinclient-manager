package org.openthinclient;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.manager.util.http.impl.HttpClientDownloadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * DownloadManagerFactory
 */
public class DownloadManagerFactory {

    private static final Logger logger = LoggerFactory.getLogger(DownloadManagerFactory.class);

    public static DownloadManager create(String serverID, NetworkConfiguration.ProxyConfiguration proxyConfiguration) {

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

        String userAgent = version == null ? serverID : serverID + "-" + version;

        return new HttpClientDownloadManager(proxyConfiguration, userAgent);
    }
}

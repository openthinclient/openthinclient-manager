package org.openthinclient;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.manager.util.http.impl.HttpClientDownloadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openthinclient.common.ApplicationVersionUtil.readVersionFromPomProperties;

/**
 * DownloadManagerFactory
 */
public class DownloadManagerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadManagerFactory.class);

    public static DownloadManager create(String serverID, NetworkConfiguration.ProxyConfiguration proxyConfiguration) {

        String version = readVersionFromPomProperties();
        LOGGER.debug("Application version is {}", version);
        String userAgent = version == null ? serverID : serverID + "-" + version;

        return new HttpClientDownloadManager(proxyConfiguration, userAgent);
    }


}

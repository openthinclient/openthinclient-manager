package org.openthinclient.manager.util.http;

import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.manager.util.http.impl.HttpClientDownloadManager;

public class DownloadManagerFactory {
  public static DownloadManager create(NetworkConfiguration.ProxyConfiguration proxyConfig) {
    return new HttpClientDownloadManager(proxyConfig);
  }
}

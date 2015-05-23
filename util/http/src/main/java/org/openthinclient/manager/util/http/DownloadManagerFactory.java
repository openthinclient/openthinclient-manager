package org.openthinclient.manager.util.http;

import org.openthinclient.manager.util.http.config.NetworkConfiguration;

public class DownloadManagerFactory {
  public static DownloadManager create(NetworkConfiguration.ProxyConfiguration proxyConfig) {
    return new ConnectToServer(proxyConfig);
  }
}

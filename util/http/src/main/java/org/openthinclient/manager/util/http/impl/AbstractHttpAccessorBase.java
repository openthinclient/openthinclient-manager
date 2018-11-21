package org.openthinclient.manager.util.http.impl;

import com.google.common.base.Strings;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.manager.util.http.config.NetworkConfiguration.ProxyConfiguration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.HttpAccessor;

/**
 * The base class for HTTP Implementations of the manager.
 */
public abstract class AbstractHttpAccessorBase extends HttpAccessor {

  private String userAgent;

  public AbstractHttpAccessorBase(NetworkConfiguration.ProxyConfiguration proxyConfig, String userAgent) {

    this.userAgent = userAgent;
    setupHttpClient(proxyConfig);

  }

  public void setupHttpClient(ProxyConfiguration proxyConfig) {

    final HttpClient httpClient;

    if (proxyConfig != null && proxyConfig.isEnabled()) {

      final HttpClientBuilder builder = HttpClients.custom() //
              .setProxy(new HttpHost(proxyConfig.getHost(), proxyConfig.getPort()));//

      builder.setUserAgent(userAgent);

      final String proxyUser = proxyConfig.getUser();
      final String proxyPassword = proxyConfig.getPassword();

      // if either a username or password is provided, we're specifying an authentication configuration
      if (!Strings.isNullOrEmpty(proxyUser) || !Strings.isNullOrEmpty(proxyPassword)) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(proxyConfig.getHost(), proxyConfig.getPort()),
                new UsernamePasswordCredentials(proxyUser, proxyPassword));
        builder.setDefaultCredentialsProvider(credsProvider);
      }

      httpClient = builder.build();

    } else {
      // as there doesn't seem to be any kind of custom configuration, we're using the default httpClient
      httpClient = HttpClients.createDefault();
    }
    setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
  }
}

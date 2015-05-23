package org.openthinclient.pkgmgr.connect;

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
import org.openthinclient.pkgmgr.I18N;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerTaskSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.HttpAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Every connection which is made from the PackageManager to the Internet to
 * download some things is made through this class.
 *
 * @author tauschfn
 */
public class ConnectToServer extends HttpAccessor {
  private static final Logger logger = LoggerFactory.getLogger(ConnectToServer.class);
  private PackageManagerTaskSummary taskSummaryManager;

  public ConnectToServer(NetworkConfiguration.ProxyConfiguration proxyConfig, PackageManagerTaskSummary taskSummary) {
    this.taskSummaryManager = taskSummary;

    final HttpClient httpClient;

    if (proxyConfig != null && proxyConfig.isEnabled()) {

      final HttpClientBuilder builder = HttpClients.custom() //
              .setProxy(new HttpHost(proxyConfig.getHost(), proxyConfig.getPort()));//

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

  public InputStream getInputStream(String adress) throws PackageManagerException {
    try {
      return getInputStream(new URI(adress));
    } catch (URISyntaxException e) {
      final String message = I18N.getMessage("ProxyManager.getInputStreamByProxy.IOException.incorrectUrl");
      if (null != taskSummaryManager) {
        taskSummaryManager.addWarning(message);
      }
      logger.error(message, e);
      throw new PackageManagerException(message, e);
    }
  }

  public InputStream getInputStream(URL url) throws PackageManagerException {
    try {
      return getInputStream(url.toURI());
    } catch (URISyntaxException e) {
      final String message = I18N.getMessage("ProxyManager.getInputStreamByProxy.IOException.incorrectUrl");
      if (null != taskSummaryManager) {
        taskSummaryManager.addWarning(message);
      }
      logger.error(message, e);
      throw new PackageManagerException(message, e);
    }
  }

  public InputStream getInputStream(URI uri) throws PackageManagerException {

    try {
      final ClientHttpRequest request = createRequest(uri, HttpMethod.GET);
      final ClientHttpResponse response = request.execute();

      return response.getBody();
    } catch (IOException e) {
      final String message = I18N.getMessage("ProxyManager.getInputStreamByProxy.IOException.connect");
      if (null != taskSummaryManager) {
        taskSummaryManager.addWarning(message);
      }
      logger.error(message, e);
      throw new PackageManagerException(message, e);
    }

  }

}

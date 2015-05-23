package org.openthinclient.manager.util.http.impl;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.openthinclient.manager.util.http.DownloadException;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.HttpAccessor;

import java.io.File;
import java.io.FileOutputStream;
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
public class HttpClientDownloadManager extends HttpAccessor implements DownloadManager {
  private static final Logger logger = LoggerFactory.getLogger(HttpClientDownloadManager.class);

  public HttpClientDownloadManager(NetworkConfiguration.ProxyConfiguration proxyConfig) {

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


  @Override
  public <T> T download(URI uri, DownloadProcessor<T> processor) throws DownloadException {

    final ClientHttpRequest request;
    try {
      request = createRequest(uri, HttpMethod.GET);
    } catch (IOException e) {
      throw new DownloadException("failed to create request for " + uri, e);
    }

    try (final ClientHttpResponse response = request.execute();
         final InputStream in = response.getBody()) {

      return processor.process(in);

    } catch (IOException e) {
      throw new DownloadException("download of " + uri + " failed", e);
    } catch (Exception e) {
      if (e instanceof DownloadException) {
        throw (DownloadException) e;
      }
      throw new DownloadException("download failed of " + uri + " failed", e);
    }


  }

  @Override
  public void downloadTo(URI uri, File targetFile) throws DownloadException {

    final ClientHttpRequest request;
    try {
      request = createRequest(uri, HttpMethod.GET);
    } catch (IOException e) {
      throw new DownloadException("failed to create request", e);
    }

    try (
            final ClientHttpResponse response = request.execute();
            final InputStream in = response.getBody();
            final FileOutputStream out = new FileOutputStream(targetFile)) {
      ByteStreams.copy(in, out);
    } catch (IOException e) {
      final String message = "downloading from " + uri + " to " + targetFile.getAbsolutePath() + " failed";
      logger.error(message, e);
      throw new DownloadException(message, e);
    }


  }

  @Override
  public void downloadTo(URL url, File targetFile) throws DownloadException {
    try {
      downloadTo(url.toURI(), targetFile);
    } catch (URISyntaxException e) {
      throw new DownloadException(e);
    }
  }

}

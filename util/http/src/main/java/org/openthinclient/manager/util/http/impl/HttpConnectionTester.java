package org.openthinclient.manager.util.http.impl;

import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

public class HttpConnectionTester extends AbstractHttpAccessorBase {

  public HttpConnectionTester(NetworkConfiguration.ProxyConfiguration proxyConfig) {
    super(proxyConfig, HttpConnectionTester.class.getName());
  }

  public Result verifyConnectivity(Request testRequest) {
    final ClientHttpRequest request;
    try {
      request = createRequest(testRequest.getUri(), testRequest.getMethod());
    } catch (IOException e) {
      throw new IllegalArgumentException("request creation failed", e);
    }

    try (final ClientHttpResponse response = request.execute()) {

      // we're accepting any 2xx and 3xx redirection code as a successful response.
      final boolean responseCodeSuccess = response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection();

      if (responseCodeSuccess)
        return Result.success();
      else
        return Result.error("Unexpected HTTP Status code received " + response.getRawStatusCode() + ". (" + response.getStatusText() + ")");

    } catch (Exception e) {
      final String message = "failed to connect to " + testRequest.getUri();
      logger.error(message, e);
      return Result.error(message, e);
    }

  }

  public static class Request {
    private final HttpMethod method;
    private final URI uri;

    public Request(URI uri) {
      this(uri, HttpMethod.GET);
    }

    public Request(URI uri, HttpMethod method) {
      this.method = method;
      this.uri = uri;
    }

    public HttpMethod getMethod() {
      return method;
    }

    public URI getUri() {
      return uri;
    }
  }

  public static class Result {
    private final boolean success;
    private final String message;
    private final Throwable error;

    private Result(boolean success, String message, Throwable error) {
      this.success = success;
      this.message = message;
      this.error = error;
    }

    public static Result success() {
      return new Result(true, null, null);
    }

    public static Result error(String message) {
      return error(message, null);
    }

    public static Result error(String message, Throwable error) {
      return new Result(false, message, error);
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

  }

}

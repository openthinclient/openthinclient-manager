package org.openthinclient.manager.util.http.impl;

import com.google.common.io.ByteStreams;

import org.openthinclient.manager.util.http.DownloadException;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.NotFoundException;
import org.openthinclient.manager.util.http.StatusCodeException;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class HttpClientDownloadManager extends AbstractHttpAccessorBase implements DownloadManager {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientDownloadManager.class);

    public HttpClientDownloadManager(NetworkConfiguration.ProxyConfiguration proxyConfig) {
        super(proxyConfig);
    }


    @Override
    public <T> T download(URI uri, DownloadProcessor<T> processor) throws DownloadException {

        final ClientHttpRequest request;
        try {
            request = createRequest(uri, HttpMethod.GET);
        } catch (IOException e) {
            throw new DownloadException("failed to create request for " + uri, e);
        }

        try (final ClientHttpResponse response = request.execute()) {

            ensureSuccessful(uri, response);

            try (final InputStream in = response.getBody()) {
                return processor.process(in);
            }

        } catch (IOException e) {
            throw new DownloadException("download of " + uri + " failed", e);
        } catch (Exception e) {
            if (e instanceof DownloadException) {
                throw (DownloadException) e;
            }
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            throw new DownloadException("download failed of " + uri + " failed", e);
        }


    }

    @Override
    public <T> T download(URL url, DownloadProcessor<T> processor) throws DownloadException {
        try {
            return download(url.toURI(), processor);
        } catch (URISyntaxException e) {
            throw new DownloadException(e);
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

        try (final ClientHttpResponse response = request.execute()) {

            ensureSuccessful(uri, response);

            try (final InputStream in = response.getBody();
                final FileOutputStream out = new FileOutputStream(targetFile)) {
                ByteStreams.copy(in, out);
            }
        } catch (IOException e) {
            final String message = "downloading from " + uri + " to " + targetFile.getAbsolutePath() + " failed";
            logger.error(message, e);
            throw new DownloadException(message, e);
        }


    }

    private void ensureSuccessful(URI uri, ClientHttpResponse response) throws IOException {
        final HttpStatus statusCode = response.getStatusCode();
        if (statusCode == HttpStatus.NOT_FOUND)
            throw new NotFoundException(uri, statusCode.value(), statusCode.getReasonPhrase());

        if (!statusCode.is2xxSuccessful()) {
            throw new StatusCodeException(uri, statusCode.value(), statusCode.getReasonPhrase());
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

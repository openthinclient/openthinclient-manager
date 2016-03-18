package org.openthinclient.manager.util.http;

import java.net.URI;

public class StatusCodeException extends DownloadException {
    private final URI uri;
    private final int statusCode;
    private final String reasonPhrase;

    public StatusCodeException(URI uri, int statusCode, String reasonPhrase) {
        super(uri + " failed: " + statusCode + " " + reasonPhrase);
        this.uri = uri;
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public URI getUri() {
        return uri;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }
}

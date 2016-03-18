package org.openthinclient.manager.util.http;

import java.net.URI;

public class NotFoundException extends StatusCodeException {
    public NotFoundException(URI uri, int statusCode, String reasonPhrase) {
        super(uri, statusCode, reasonPhrase);
    }
}

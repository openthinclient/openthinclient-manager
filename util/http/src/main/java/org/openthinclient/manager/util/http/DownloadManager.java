package org.openthinclient.manager.util.http;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public interface DownloadManager {
  InputStream getInputStream(URL url) throws DownloadException;

  InputStream getInputStream(URI uri) throws DownloadException;
}

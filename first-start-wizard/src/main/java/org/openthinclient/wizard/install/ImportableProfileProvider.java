package org.openthinclient.wizard.install;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.manager.util.http.DownloadManager;

import java.io.InputStream;
import java.net.URI;

public class ImportableProfileProvider {

  private final URI baseURL;

  public ImportableProfileProvider(URI baseURL) {
    this.baseURL = baseURL;
  }

  public AbstractProfileObject access(InstallContext context, ImportItem item) throws Exception {

    final URI path = createTargetURI(item);

    if (requiresHttpDownload(path)) {

      final DownloadManager downloadManager = context.getDownloadManager();
      if (downloadManager == null)
        throw new IllegalStateException("To access the " + item + " from " + path + " a download manager is required. No download manager is currently available");
      return downloadManager.download(path, in -> read(in, item));

    } else {

      try (final InputStream in = path.toURL().openStream()) {
        return read(in, item);
      }
    }
  }

  private AbstractProfileObject read(InputStream in, ImportItem item) throws java.io.IOException {
    final ObjectMapper mapper = getMapper();
    return mapper.readValue(in, item.getTargetType());
  }

  private ObjectMapper getMapper() {
    return new ObjectMapper();
  }

  protected URI createTargetURI(ImportItem item) {
    return baseURL.resolve(item.getPath());
  }


  protected boolean requiresHttpDownload(URI uri) {

    return uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https");

  }


}

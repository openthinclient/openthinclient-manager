package org.openthinclient.api.distributions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openthinclient.api.context.InstallContext;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.progress.ProgressReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;

public class ImportableProfileProvider {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final URI baseURL;

  public ImportableProfileProvider(URI baseURL) {
    this.baseURL = baseURL;
    log.info("Initialize ImportableProfileProvider with baseURL " + baseURL);
  }

  public AbstractProfileObject access(InstallContext context, ImportItem item, ProgressReceiver progressReceiver) throws Exception {

    final URI path = createTargetURI(item);
    log.info("Import profile from " + path);

    if (requiresHttpDownload(path)) {
      final DownloadManager downloadManager = context.getDownloadManager();
      if (downloadManager == null)
        throw new IllegalStateException("To access the " + item + " from " + path + " a download manager is required. No download manager is currently available");

      return downloadManager.download(path, in -> read(in, item), progressReceiver);
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

  public URI createTargetURI(ImportItem item) {
    return baseURL.resolve(item.getPath());
  }


  public boolean requiresHttpDownload(URI uri) {

    if (uri.getScheme() == null) {
      return false;
    }
    return uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https");

  }


}

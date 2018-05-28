package org.openthinclient.pkgmgr.exception;

import java.net.URL;
import org.openthinclient.pkgmgr.PackageManagerException;

public class PackageManagerDownloadException extends PackageManagerException {

  /** serialVersionUID */
  private static final long serialVersionUID = 360924013969609905L;

  private URL downloadUrl;

  public PackageManagerDownloadException(String message, URL url, Exception exception) {
    super(message, exception);
    this.downloadUrl = url;
  }

  public URL getDownloadUrl() {
    return downloadUrl;
  }
}

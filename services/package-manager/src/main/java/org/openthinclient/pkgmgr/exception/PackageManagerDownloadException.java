package org.openthinclient.pkgmgr.exception;

import java.io.IOException;

public class PackageManagerDownloadException extends Exception {

  /** serialVersionUID */
  private static final long serialVersionUID = 360924013969609905L;

  public PackageManagerDownloadException(IOException exception) {
    super(exception);
  }
}

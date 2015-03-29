package org.openthinclient.pkgmgr;

public class SourcesListException extends RuntimeException {
  public SourcesListException() {
  }

  public SourcesListException(String message) {
    super(message);
  }

  public SourcesListException(String message, Throwable cause) {
    super(message, cause);
  }

  public SourcesListException(Throwable cause) {
    super(cause);
  }

}

package org.openthinclient.manager.util.http;

public class DownloadException extends RuntimeException {
  public DownloadException() {
  }

  public DownloadException(String message) {
    super(message);
  }

  public DownloadException(String message, Throwable cause) {
    super(message, cause);
  }

  public DownloadException(Throwable cause) {
    super(cause);
  }

}

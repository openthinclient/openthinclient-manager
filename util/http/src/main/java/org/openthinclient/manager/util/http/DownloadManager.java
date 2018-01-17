package org.openthinclient.manager.util.http;

import org.openthinclient.progress.ProgressReceiver;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public interface DownloadManager {
  <T> T download(URI uri, DownloadProcessor<T> processor, ProgressReceiver progressReceiver) throws DownloadException;

  <T> T download(URL url, DownloadProcessor<T> processor, ProgressReceiver progressReceiver) throws DownloadException;

  void downloadTo(URI uri, File targetFile) throws DownloadException;

  void downloadTo(URL url, File targetFile) throws DownloadException;

  interface DownloadProcessor<T> {
    T process(InputStream in) throws Exception;
  }
}

package org.openthinclient.pkgmgr;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.resource.ClassPathResourceManager;

import java.net.MalformedURLException;
import java.net.URL;

public class DebianTestRepositoryServer {


  private Undertow undertow;
  private URL url;

  public static void main(String[] args) {
    new DebianTestRepositoryServer().start();
  }

  public synchronized void start() {
    if (undertow != null) {
      throw new IllegalStateException("already started");
    }
    final int port = 19090;
    this.url = createUrl(port);
    this.undertow = Undertow.builder() //
            .addHttpListener(port, null) //
            .setHandler(Handlers.resource( //
                    new ClassPathResourceManager(getClass().getClassLoader(), "test-repository/") //
            ).setDirectoryListingEnabled(true))//
            .build();
    undertow.start();
  }

  private URL createUrl(int port) {
    try {
      return new URL("http", "localhost", port, "");
    } catch (MalformedURLException e) {
      throw new RuntimeException("Failed to create server access URL", e);
    }
  }

  public URL getServerUrl() {
    if (url == null) {
      throw new IllegalStateException("not started");
    }
    return url;
  }

  public synchronized void stop() {
    if (undertow == null) {
      throw new IllegalStateException("not started");
    }
    undertow.stop();
    undertow = null;
    url = null;
  }
}

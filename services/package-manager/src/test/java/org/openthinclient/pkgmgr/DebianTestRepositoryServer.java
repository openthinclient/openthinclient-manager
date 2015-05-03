package org.openthinclient.pkgmgr;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.resource.ClassPathResourceManager;

public class DebianTestRepositoryServer {


  private Undertow undertow;

  public static void main(String[] args) {
    new DebianTestRepositoryServer().start();
  }

  public synchronized void start() {
    if (undertow != null) {
      throw new IllegalStateException("already started");
    }
    this.undertow = Undertow.builder() //
            .addHttpListener(9090, null) //
            .setHandler(Handlers.resource( //
                    new ClassPathResourceManager(getClass().getClassLoader(), "test-repository/") //
            ).setDirectoryListingEnabled(true))//
            .build();
    undertow.start();
  }

  public synchronized void stop() {
    if (undertow == null) {
      throw new IllegalStateException("not started");
    }
    undertow.stop();
    undertow = null;
  }
}

package org.openthinclient.splash;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.io.IoCallback;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.AllowedMethodsHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import io.undertow.util.HttpString;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioWorker;


public enum SplashServer {
  INSTANCE();

  private final Logger LOG = LoggerFactory.getLogger(SplashServer.class);

  private final static float MAX_BEANS = 689;

  private int beansLoaded = 0;
  private String progress = "0";
  private ServerSentEventHandler sseHandler;
  private Undertow server;

  private SplashServer() {
    HttpHandler statusHandler = ex ->
        ex.getResponseSender().send("STARTING", IoCallback.END_EXCHANGE);

    sseHandler = Handlers.serverSentEvents((connection, lastID) -> {
      connection.sendRetry(1000);
      connection.send(progress, "progress", null, null);
    });

    HttpHandler serviceUnavailableHandler = ex -> {
      ex.setStatusCode(503);
    };

    ResourceManager rm = new ClassPathResourceManager(SplashServer.class.getClassLoader());
    HttpHandler handler = Handlers.path(resourceFile(rm, "splash.html"))
                          .addPrefixPath("/api/v2/server-status", statusHandler)
                          .addPrefixPath("/api/v2/startup-progress", sseHandler)
                          .addPrefixPath("/api/", serviceUnavailableHandler)
                          .addExactPath("/favicon.ico", resourceFile(rm, "favicon.ico"));

    server = Undertow.builder()
              .addHttpListener(8080, "0.0.0.0")
              .setHandler(new AllowedMethodsHandler(handler, new HttpString("GET")))
              .build();
  }

  public static SplashServer getInstance() {
    return INSTANCE;
  }

  public void start() {
    LOG.info("Starting splash server");
    server.start();
  }

  public void stop()  {
    LOG.info("Stopping splash server after {} beans", beansLoaded);
    sseHandler.getConnections().forEach(conn -> {
      conn.send(null, "close", null, null);
      try {
        conn.close();
      } catch (IOException ex) {
        LOG.error("Failed to stop splash server", ex);
      }
    });
    // Get the worker now. getWorker() return null after stop()
    XnioWorker worker = server.getWorker();
    server.stop();
    try {
      worker.awaitTermination();
    } catch (InterruptedException ex) {
      return;
    }
    sseHandler.getConnections().forEach(conn -> {
      try {
        conn.close();
      } catch (IOException ex) {}
    });
  }

  public void beanLoaded() {
    beansLoaded++;
    progress = String.valueOf(Math.min(1, beansLoaded/MAX_BEANS));
    sseHandler.getConnections().forEach(conn -> conn.send(progress, "progress", null, null));
  }

  private HttpHandler resourceFile(ResourceManager rm, String resourcePath) {
    try {
      Resource resource = rm.getResource(resourcePath);
      return ex -> resource.serve(ex.getResponseSender(), ex, IoCallback.END_EXCHANGE);
    } catch (IOException err) {
      LOG.error("Failed to load resource {}", resourcePath, err);
      return ex -> ex.setStatusCode(501).getResponseSender().send(err.toString());
    }
  }
}

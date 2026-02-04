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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioWorker;


public enum SplashServer {
  INSTANCE();

  private final Logger LOG = LoggerFactory.getLogger(SplashServer.class);

  private final static float MAX_BEANS = 695;

  private int beansLoaded = 0;
  private String progress = "0";
  private ServerSentEventHandler sseHandler;
  private Undertow server;
  private boolean updatingPackages = false;

  private SplashServer() {
    HttpHandler statusHandler = ex ->
        ex.getResponseSender().send(
            updatingPackages ? "UPDATING_OS" : "STARTING",
            IoCallback.END_EXCHANGE);

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

    Properties externalProps = loadExternalApplicationProperties();
    int port = 8080;
    try {
      port = Integer.parseInt(externalProps.getProperty("server.port", "8080"));
    } catch (NumberFormatException ex) {
      LOG.error("Invalid port in external application.properties");
    }
    String host = externalProps.getProperty("server.address", "0.0.0.0");

    server = Undertow.builder()
              .addHttpListener(port, host)
              .setHandler(new AllowedMethodsHandler(handler, new HttpString("GET")))
              .build();
  }

  private Properties loadExternalApplicationProperties() {
    Properties props = new Properties();
    try (FileInputStream fis = new FileInputStream("application.properties")) {
      props.load(fis);
    } catch (FileNotFoundException ex) { // ignore and use defaults
    } catch (IOException ex) {
      LOG.error("Failed to read external application.properties", ex);
    }
    return props;
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
    closeSSEConnections();
  }

  public void beanLoaded() {
    beansLoaded++;
    setProgress(beansLoaded/MAX_BEANS);
  }

  public void setProgress(double newProgress) {
    progress = String.valueOf(Math.min(1, newProgress));
    sseHandler.getConnections().forEach(conn ->
        conn.send(progress, "progress", null, null));
  }

  public void setUpdatingPackages(boolean updatingPackages) {
    this.updatingPackages = updatingPackages;
    this.progress = "0";
    closeSSEConnections();  // close to force status update in splash.html
  }

  private void closeSSEConnections() {
    sseHandler.getConnections().forEach(conn -> {
      conn.send(null, "close", null, null);
      try {
        conn.close();
      } catch (Exception ex) {}
    });
  }

  private HttpHandler resourceFile(ResourceManager rm, String resourcePath) {
    try {
      Resource resource = rm.getResource(resourcePath);
      return ex -> {
        ex.getResponseHeaders().add(
          new HttpString("Cache-Control"),
          "no-store, no-cache, max-age=0, must-revalidate, proxy-revalidate"
        );
        resource.serve(ex.getResponseSender(), ex, IoCallback.END_EXCHANGE);
      };
    } catch (IOException err) {
      LOG.error("Failed to load resource {}", resourcePath, err);
      return ex -> ex.setStatusCode(501).getResponseSender().send(err.toString());
    }
  }
}

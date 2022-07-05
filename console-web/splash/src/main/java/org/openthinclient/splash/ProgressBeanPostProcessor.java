package org.openthinclient.splash;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;

/**
 * Observe bean loading, inform SplashServer about bean initialization and stop
 * SplashServer when the "real" WebServer starts.
 */
public class ProgressBeanPostProcessor implements BeanPostProcessor {

  public Object postProcessBeforeInitialization(Object bean, String beanName) {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
  SplashServer.INSTANCE.beanLoaded();
    if (bean instanceof ServletWebServerFactory) {
    return wrapWebServer((ServletWebServerFactory) bean);
    } else {
      return bean;
    }
  }

  private ServletWebServerFactory wrapWebServer(ServletWebServerFactory factory) {
    return initializers -> new WebServerWrapper(factory.getWebServer(initializers));
  }
}

/**
 * Wrapper around the actual WebServer instance that stops the SplashServer when
 * WebServer::start is called.
 */
class WebServerWrapper implements WebServer {
  WebServer webServer;

  WebServerWrapper(WebServer webServer) {
    this.webServer = webServer;
  }

  @Override
  public int getPort() {
    return webServer.getPort();
  }

  @Override
  public void start() throws WebServerException {
    SplashServer.INSTANCE.stop();
    webServer.start();
  }

  @Override
  public void stop() throws WebServerException {
    webServer.stop();
  }
}

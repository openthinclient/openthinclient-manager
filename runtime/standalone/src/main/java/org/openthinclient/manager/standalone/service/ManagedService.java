package org.openthinclient.manager.standalone.service;

import org.openthinclient.service.common.Service;
import org.openthinclient.service.common.home.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

public class ManagedService<S extends Service<C>, C extends Configuration> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ManagedService.class);
  private final ApplicationEventPublisher eventPublisher;
  private final Service<?> service;
  private volatile boolean running;
  private volatile Exception lastException;

  public ManagedService(ApplicationEventPublisher eventPublisher, Service<?> service) {
    this.eventPublisher = eventPublisher;
    this.service = service;
  }

  public synchronized void start() {
    if (running) {
      LOGGER.error("Not starting service " + service + " as it is already running");
      return;
    }
    running = true;
    lastException = null;
    try {
      service.startService();
      eventPublisher.publishEvent(new ServiceStartedApplicationEvent(this));
    } catch (Exception e) {
      lastException = e;
      LOGGER.error("Failed to start service " + service, e);
      eventPublisher.publishEvent(new ServiceErrorApplicationEvent(this, e));
      running = false;
    }
  }

  public synchronized void stop() {
    if (!running) {
      LOGGER.error("Can not stop service " + service + " as it is not running");
      return;
    }

    running = false;
    try {
      service.stopService();
      eventPublisher.publishEvent(new ServiceStoppedApplicationEvent(this));
    } catch (Exception e) {
      LOGGER.error("Failed to stop service " + service, e);
    }
  }

  public Service<?> getService() {
    return service;
  }

  public boolean isRunning() {
    return running;
  }

  /**
   * Returns the {@link Exception} caused during a failed startup of the service.
   *
   * @return either a {@link Exception} or <code>null</code> if the service startup was successful.
   */
  public Exception getStartupException() {
    return lastException;
  }

  /**
   * Determines whether or not this service has failed to start.
   *
   * @return <code>true</code> if the service startup failed, <code>false</code> otherwise
   */
  public boolean isFaulty() {
    return getStartupException() != null;
  }
}

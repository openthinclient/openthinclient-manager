package org.openthinclient.service.common;

import org.openthinclient.service.common.home.ManagerHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

public class ManagedService<S extends Service<C>, C extends ServiceConfiguration> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ManagedService.class);
  private final ApplicationEventPublisher eventPublisher;
  private final ManagerHome managerHome;
  private final S service;
  private volatile boolean running;
  private volatile Exception lastException;

  public ManagedService(ApplicationEventPublisher eventPublisher, ManagerHome managerHome, S service) {
    this.eventPublisher = eventPublisher;
    this.managerHome = managerHome;
    this.service = service;

    // ensure the configuration for the service is initialized as early as possible.
    loadConfiguration();
  }

  /**
   * Read the appropriate configuration for the {@link Service}
   */
  private void loadConfiguration() {
    final C configuration = managerHome.getConfiguration(service.getConfigurationClass());
    //noinspection unchecked
    service.setConfiguration(configuration);
  }

  public synchronized void start() {
    if (running) {
      LOGGER.error("Not starting service " + service + " as it is already running");
      return;
    }
    running = true;
    lastException = null;
    try {

      // prior to every start, we have to read the configuration.
      // The start might be due to a restart of the service, after configuration changes have been made.
      loadConfiguration();
      service.startService();
      eventPublisher.publishEvent(new ServiceStartedEvent(this));
    } catch (Exception e) {
      lastException = e;
      LOGGER.error("Failed to start service " + service, e);
      eventPublisher.publishEvent(new ServiceErrorEvent(this, e));
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
      eventPublisher.publishEvent(new ServiceStoppedEvent(this));
    } catch (Exception e) {
      LOGGER.error("Failed to stop service " + service, e);
    }
  }

  public synchronized void restart() {
    if (running)
      stop();
    start();
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

  /**
   * Whether or not the {@link Service} shall be started during initial system startup.
   *
   * @return {@code true} if the {@link Service} shall be started
   */
  public boolean isAutostartEnabled() {
    return service.getConfiguration().isAutostartEnabled();
  }
}

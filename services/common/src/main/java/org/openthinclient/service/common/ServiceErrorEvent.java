package org.openthinclient.service.common;

import org.springframework.context.ApplicationEvent;

public class ServiceErrorEvent extends ApplicationEvent {
  private final Throwable error;

  public ServiceErrorEvent(ManagedService service, Throwable error) {
    super(service);
    this.error = error;
  }

  public ManagedService getService() {
    return (ManagedService) getSource();
  }

  public Throwable getError() {
    return error;
  }
}

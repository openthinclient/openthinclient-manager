package org.openthinclient.service.common;

import org.springframework.context.ApplicationEvent;

public class ServiceStoppedEvent extends ApplicationEvent {
  public ServiceStoppedEvent(ManagedService service) {
    super(service);
  }

  public ManagedService getService() {
    return (ManagedService) getSource();
  }
}

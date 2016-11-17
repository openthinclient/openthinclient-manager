package org.openthinclient.service.common;

import org.springframework.context.ApplicationEvent;

public class ServiceStartedEvent extends ApplicationEvent {
  public ServiceStartedEvent(ManagedService service) {
    super(service);
  }

  public ManagedService getService() {
    return (ManagedService) getSource();
  }
}

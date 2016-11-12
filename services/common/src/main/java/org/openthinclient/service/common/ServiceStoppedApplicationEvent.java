package org.openthinclient.service.common;

import org.springframework.context.ApplicationEvent;

public class ServiceStoppedApplicationEvent extends ApplicationEvent {
  public ServiceStoppedApplicationEvent(ManagedService service) {
    super(service);
  }

  public ManagedService getService() {
    return (ManagedService) getSource();
  }
}

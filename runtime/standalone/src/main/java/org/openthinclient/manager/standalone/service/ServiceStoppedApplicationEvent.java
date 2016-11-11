package org.openthinclient.manager.standalone.service;

import org.springframework.context.ApplicationEvent;

public class ServiceStoppedApplicationEvent extends ApplicationEvent {
  public ServiceStoppedApplicationEvent(ManagedService service) {
    super(service);
  }

  public ManagedService getService() {
    return (ManagedService) getSource();
  }
}

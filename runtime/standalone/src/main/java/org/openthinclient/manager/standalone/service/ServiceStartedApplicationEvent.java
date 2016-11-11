package org.openthinclient.manager.standalone.service;

import org.springframework.context.ApplicationEvent;

public class ServiceStartedApplicationEvent extends ApplicationEvent {
  public ServiceStartedApplicationEvent(ManagedService service) {
    super(service);
  }

  public ManagedService getService() {
    return (ManagedService) getSource();
  }
}

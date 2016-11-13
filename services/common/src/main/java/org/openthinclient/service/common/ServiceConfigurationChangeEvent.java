package org.openthinclient.service.common;

import org.springframework.context.ApplicationEvent;

public class ServiceConfigurationChangeEvent extends ApplicationEvent {
  public ServiceConfigurationChangeEvent(ManagedService service) {
    super(service);
  }

  public ManagedService getService() {
    return (ManagedService) getSource();
  }
}

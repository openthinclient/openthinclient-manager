package org.openthinclient.service.common;

import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ManagerHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class ServiceManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceManager.class);


  private List<ManagedService> services;
  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @SuppressWarnings("unchecked")
  @Autowired
  void setServices(List<Service> services) {
    this.services = services.stream() //
            .map(service -> new ManagedService(eventPublisher, service)) //
            .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  public <S extends Service<C>, C extends Configuration> ManagedService<S, C> getManagedService(Class<S> serviceType) {
    return services.stream() //
            .filter(service -> serviceType.isAssignableFrom(service.getService().getClass())) //
            .findFirst() //
            .orElse(null);
  }


  public List<ManagedService> getManagedServices() {
    return this.services;
  }

  @PostConstruct
  public void startServices() {
    this.services.forEach(managedService -> {
      final Service service = managedService.getService();
      LOGGER.info("Starting service {}", service.getClass().getName());
      managedService.start();
    });
  }

  @PreDestroy
  public void stopServices() {
    this.services.forEach(managedService -> {
      if (managedService.isRunning()) {
        LOGGER.info("Stopping service {}", managedService.getService().getClass().getName());
        managedService.stop();
      } else {
        LOGGER.info("Skipping already stopped service {}", managedService.getService().getClass().getName());
      }
    });
  }

}

package org.openthinclient.manager.standalone.service;

import org.openthinclient.service.common.Service;
import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ManagerHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;

/**
 * Spring framework hook to control the startup and shutdown of {@link org.openthinclient.service.common.Service} instances.
 */
public class ServiceBeanPostProcessor implements DestructionAwareBeanPostProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceBeanPostProcessor.class);

  private final ManagerHome managerHome;

  public ServiceBeanPostProcessor(ManagerHome managerHome) {
    this.managerHome = managerHome;
  }

  @Override
  public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {

    if (bean instanceof Service) {
      try {
        LOG.info("Stopping service " + bean.getClass().getName());
        ((Service) bean).stopService();
      } catch (Exception e) {
        LOG.error("Failed to shutdown Service " + beanName + "(" + bean + ")", e);
      }

    }

  }

  @SuppressWarnings("unchecked")
  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

    if (bean instanceof Service) {
      // read the service configuration

      LOG.info("Starting service {}", bean.getClass().getName());

      final Configuration configuration = managerHome.getConfiguration(((Service) bean).getConfigurationClass());

      ((Service) bean).setConfiguration(configuration);

    }

    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

    if (bean instanceof Service) {

      try {
        ((Service) bean).startService();
      } catch (Exception e) {
        throw new BeanCreationException(beanName, "Failed to start service " + bean, e);
      }

    }

    return bean;
  }
}

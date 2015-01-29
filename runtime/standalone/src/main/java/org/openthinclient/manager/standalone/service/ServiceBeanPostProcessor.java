package org.openthinclient.manager.standalone.service;

import org.openthinclient.service.common.Service;
import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ConfigurationDirectory;
import org.openthinclient.service.common.home.ConfigurationFile;
import org.openthinclient.service.common.home.ManagerHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;

import java.io.File;
import java.lang.reflect.Field;

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

      final Configuration configuration = managerHome.getConfiguration(((Service) bean).getConfigurationClass());

      for (Field field : configuration.getClass().getDeclaredFields()) {
        field.setAccessible(true);

        // FIXME more type checking required! We're not validating that the field is actually of type File

        try {
          final ConfigurationFile configurationFileAnnotation = field.getAnnotation(ConfigurationFile.class);
          final ConfigurationDirectory configurationDirectoryAnnotation = field.getAnnotation(ConfigurationDirectory.class);
          if (configurationFileAnnotation != null) {
            final File configurationFile = managerHome.getConfigurationFile(configuration.getClass(), configurationFileAnnotation);
            field.set(configuration, configurationFile);

            LOG.info("configuration [FILE]: " + configuration.getClass().getSimpleName() + "." + field.getName() + ": " + configurationFile.getAbsolutePath());

          } else if (configurationDirectoryAnnotation != null) {
            final File configurationDirectory = managerHome.getConfigurationDirectory(configuration.getClass(), configurationDirectoryAnnotation);
            field.set(configuration, configurationDirectory);

            LOG.info("configuration [DIR]: " + configuration.getClass().getSimpleName() + "." + field.getName() + ": " + configurationDirectory.getAbsolutePath());
          }
        } catch (IllegalAccessException e) {
          throw new RuntimeException("Failed to initialize configuration", e);
        }

      }

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

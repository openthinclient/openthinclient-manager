package org.openthinclient.service.common.home.impl;

import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ConfigurationDirectory;
import org.openthinclient.service.common.home.ConfigurationFile;
import org.openthinclient.service.common.home.ManagerHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class DefaultManagerHome implements ManagerHome {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultManagerHome.class);

  private final Map<Class<? extends Configuration>, Configuration> configurations;

  private final File managerHomeDirectory;


  public DefaultManagerHome(File managerHomeDirectory) {
    this.managerHomeDirectory = managerHomeDirectory;

    LOG.info("Using manager home: " + managerHomeDirectory.getAbsolutePath());

    configurations = new HashMap<>();
  }

  @SuppressWarnings("unchecked")
  public synchronized <T extends Configuration> T getConfiguration(Class<T> configurationClass) {

    Configuration configuration = configurations.get(configurationClass);

    if (configuration == null) {
      // read the configuration
      final File file = getConfigurationFile(configurationClass);

      LOG.info("loading " + configurationClass.getSimpleName() + ": " + file.getAbsolutePath());
      final T instance;
      if (file.exists()) {

        instance = JAXB.unmarshal(file, configurationClass);

        LOG.info(configurationClass.getSimpleName() + " read successfully");
      } else {
        // create a default instance
        try {
          instance = configurationClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
          throw new RuntimeException("Failed to create configuration class instance", e);
        }
        LOG.info("new " + configurationClass.getSimpleName() + " instance created with defaults");
      }

      initializeConfigurationFiles(instance);

      configurations.put(configurationClass, instance);

      return instance;
    }

    return (T) configuration;
  }

  private void initializeConfigurationFiles(Configuration configuration) {
    for (Field field : configuration.getClass().getDeclaredFields()) {
      field.setAccessible(true);

      // FIXME more type checking required! We're not validating that the field is actually of type File

      try {
        final ConfigurationFile configurationFileAnnotation = field.getAnnotation(ConfigurationFile.class);
        final ConfigurationDirectory configurationDirectoryAnnotation = field.getAnnotation(ConfigurationDirectory.class);
        if (configurationFileAnnotation != null) {
          final File configurationFile = getConfigurationFile(configuration.getClass(), configurationFileAnnotation);
          field.set(configuration, configurationFile);

          final String message = "configuration [FILE]: " + configuration.getClass().getSimpleName() + "." + field.getName() + ": " + configurationFile.getAbsolutePath();
          if (!configurationFile.getParentFile().exists() && !configurationFile.getParentFile().mkdirs()) {
            LOG.error(message + " [FAIL]");
          } else {
            LOG.info(message + " [OK]");
          }

        } else if (configurationDirectoryAnnotation != null) {
          final File configurationDirectory = getConfigurationDirectory(configuration.getClass(), configurationDirectoryAnnotation);
          field.set(configuration, configurationDirectory);

          final String message = "configuration [DIR]: " + configuration.getClass().getSimpleName() + "." + field.getName() + ": " + configurationDirectory.getAbsolutePath();
          if (!configurationDirectory.exists() && !configurationDirectory.mkdirs()) {
            LOG.error(message + " [FAIL]");
          } else {
            LOG.info(message + " [OK]");
          }
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Failed to initialize configuration", e);
      }

    }

  }

  @Override
  public File getConfigurationFile(Class<? extends Configuration> configurationClass) {
    final File file = constructConfigurationFile(configurationClass);

    if (file.exists() && !file.isFile()) {
      throw new IllegalArgumentException("the configuration target location " + file.getAbsolutePath() + " is not a file");
    }
    return file;
  }

  private File constructConfigurationFile(Class<? extends Configuration> configurationClass) {
    final ConfigurationFile path = configurationClass.getAnnotation(ConfigurationFile.class);

    if (path == null) {
      throw new IllegalArgumentException("The provided configuration class " + configurationClass + " does not define a @" + ConfigurationFile.class.getSimpleName() + " annotation.");
    }


    return new File(managerHomeDirectory, path.value());
  }

  @Override
  public File getConfigurationDirectory(Class<? extends Configuration> configurationClass, ConfigurationDirectory relativeConfigurationDirectory) {
    final File configurationFile = constructConfigurationFile(configurationClass);

    final File directory = new File(configurationFile.getParentFile(), relativeConfigurationDirectory.value());

    if (directory.exists() && !directory.isDirectory()) {
      // FIXME stupid message!
      throw new IllegalArgumentException("Expected directory: " + directory.getAbsolutePath());
    }

    directory.mkdirs();
    return directory;
  }

  @Override
  public File getConfigurationFile(Class<? extends Configuration> configurationClass, ConfigurationFile relativeConfigurationPath) {
    final File configurationFile = constructConfigurationFile(configurationClass);

    final File file = new File(configurationFile.getParentFile(), relativeConfigurationPath.value());

    if (file.exists() && !file.isFile()) {
      // FIXME stupid message!
      throw new IllegalArgumentException("Expected file: " + file.getAbsolutePath());
    }
    return file;
  }

  @Override
  public synchronized void save(Class<? extends Configuration> configurationClass) {
    if (configurationClass == null) {
      throw new IllegalArgumentException("configurationClass must not be null");
    }

    final File configurationFile = getConfigurationFile(configurationClass);

    // create the target directories
    // FIXME check if the directories have been created successfully
    configurationFile.getParentFile().mkdirs();

    JAXB.marshal(configurations.get(configurationClass), configurationFile);
  }

  @Override
  public synchronized void saveAll() {
    // hand all registered configurations to save(Class)
    configurations.keySet().forEach(this::save);
  }

  @Override
  public File getLocation() {
    return managerHomeDirectory;
  }
}

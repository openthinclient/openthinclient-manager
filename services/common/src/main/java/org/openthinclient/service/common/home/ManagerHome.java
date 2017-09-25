package org.openthinclient.service.common.home;

import java.io.File;

/**
 * A model representation of the openthinclient manager home directory.
 */
public interface ManagerHome {

  ManagerHomeMetadata getMetadata();

  <T extends Configuration> T getConfiguration(Class<T> configurationClass);

  void saveAll();

  void save(Class<? extends Configuration> configurationClass);

  File getConfigurationFile(Class<? extends Configuration> configurationClass);
  File getConfigurationFile(Class<? extends Configuration> configurationClass, ConfigurationFile relativeConfigurationPath);
  File getConfigurationDirectory(Class<? extends Configuration> configurationClass, ConfigurationDirectory relativeConfigurationDirectory);

  /**
   * Returns the location of the manager home
   *
   * @return the location of the manager home
   */
  File getLocation();
}

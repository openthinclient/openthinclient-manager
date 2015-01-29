package org.openthinclient.service.common.home;

import java.io.File;

/**
 * A model representation of the openthinclient manager home directory.
 */
public interface ManagerHome {

  public <T extends Configuration> T getConfiguration(Class<T> configurationClass);

  public void saveAll();

  public void save(Class<? extends Configuration> configurationClass);

  File getConfigurationFile(Class<? extends Configuration> configurationClass);
  File getConfigurationFile(Class<? extends Configuration> configurationClass, ConfigurationFile relativeConfigurationPath);
  File getConfigurationDirectory(Class<? extends Configuration> configurationClass, ConfigurationDirectory relativeConfigurationDirectory);
}

package org.openthinclient.service.common;

public interface Service<CONF extends ServiceConfiguration> {

  void setConfiguration(CONF configuration);

  CONF getConfiguration();

  Class<CONF> getConfigurationClass();

  void startService() throws Exception;

  void stopService() throws Exception;

}

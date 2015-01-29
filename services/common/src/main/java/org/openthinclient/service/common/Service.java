package org.openthinclient.service.common;

public interface Service<CONF> {

  void setConfiguration(CONF configuration);

  CONF getConfiguration();

  Class<CONF> getConfigurationClass();

  public void startService() throws Exception;

  public void stopService() throws Exception;

}

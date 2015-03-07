package org.openthinclient.service.common;

import org.openthinclient.service.common.home.Configuration;

public interface Service<CONF extends Configuration> {
public interface Service<CONF extends Configuration> {

  void setConfiguration(CONF configuration);

  CONF getConfiguration();

  Class<CONF> getConfigurationClass();

  public void startService() throws Exception;

  public void stopService() throws Exception;

}

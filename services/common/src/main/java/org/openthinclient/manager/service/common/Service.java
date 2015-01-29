package org.openthinclient.manager.service.common;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public interface Service {

  @PostConstruct
  public void startService() throws Exception;

  @PreDestroy
  public void stopService() throws Exception;

}

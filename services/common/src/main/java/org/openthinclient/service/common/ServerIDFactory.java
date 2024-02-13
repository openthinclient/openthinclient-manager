package org.openthinclient.service.common;

import java.util.UUID;

public class ServerIDFactory {

  private ServerIDFactory() {
  }

  public static String create() {
    return UUID.randomUUID().toString();
  }
}

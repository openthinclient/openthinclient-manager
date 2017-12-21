package org.openthinclient.service.common;

import java.util.UUID;

public class ServerIDFactory {

  private ServerIDFactory() {
  }

  /**
   * Creates a new server id that is most likely globally unique. ServerID clashes are possible, but
   * highly unlikely.
   */
  public static String create() {
    return UUID.randomUUID().toString();
  }
}

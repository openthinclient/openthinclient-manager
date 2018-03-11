package org.openthinclient.service.common;

import org.openthinclient.service.common.home.Configuration;

/**
 * Common interface for all {@link Service service configurations}.
 */
public interface ServiceConfiguration extends Configuration {

  /**
   * Whether or not the {@link Service} shall be started during initial system startup. The result
   * of this method will be used by {@link ManagedService#isAutostartEnabled()}.
   *
   * @return {@code true} if the {@link Service} shall be started
   */
  default boolean isAutostartEnabled() {
    // all services should be started automatically. In some cases disabling might be useful
    return true;
  }

}

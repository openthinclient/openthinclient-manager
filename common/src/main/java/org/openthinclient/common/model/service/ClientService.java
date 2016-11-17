package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Client;

import java.util.Set;

public interface ClientService {

  String DEFAULT_CLIENT_MAC = "00:00:00:00:00:00";

  Set<Client> findByHwAddress(String hwAddressString);

  Set<Client> findAll();

  /**
   * Accesses the {@link Client default client}, that shall be used if no specialized configuration
   * has been found. The default client  must be configured using the MAC-Address {@link
   * #DEFAULT_CLIENT_MAC 00:00:00:00:00:00}.
   *
   * @return the default client configuration, or <code>null</code> if no default client has been
   * configured
   */
  Client getDefaultClient();
}

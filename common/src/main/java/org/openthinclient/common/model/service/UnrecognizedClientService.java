package org.openthinclient.common.model.service;

import org.openthinclient.common.model.UnrecognizedClient;

import java.util.Set;

public interface UnrecognizedClientService {
  Set<UnrecognizedClient> findByHwAddress(String hwAddressString);

  Set<UnrecognizedClient> findAll();

  UnrecognizedClient add(UnrecognizedClient unrecognizedClient);

}

package org.openthinclient.common.model.service;

import org.openthinclient.common.model.UnrecognizedClient;

import java.util.Set;
import java.util.List;

public interface UnrecognizedClientService extends DirectoryObjectService<UnrecognizedClient> {
  Set<UnrecognizedClient> findByHwAddress(String hwAddressString);

  Set<UnrecognizedClient> findAll();

  UnrecognizedClient add(UnrecognizedClient unrecognizedClient);

  public List<String> getLastSeenMACs(long amount);

}

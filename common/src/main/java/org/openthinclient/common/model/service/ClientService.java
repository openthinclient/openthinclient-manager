package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Client;

import java.util.Set;

public interface ClientService {

  Set<Client> findByHwAddress(String hwAddressString);

  Set<Client> findAll();

}

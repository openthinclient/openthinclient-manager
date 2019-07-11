package org.openthinclient.common.model.service;

import org.openthinclient.common.model.FlatClient;

import java.util.Set;

public interface FlatClientService extends DirectoryObjectService<FlatClient> {

  Set<FlatClient> findAll();

}

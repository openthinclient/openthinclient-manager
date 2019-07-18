package org.openthinclient.common.model.service;

import org.openthinclient.common.model.ClientMeta;

import java.util.Set;

public interface ClientMetaService extends DirectoryObjectService<ClientMeta> {

  Set<ClientMeta> findAll();

}

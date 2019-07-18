package org.openthinclient.common.model.service;

import org.openthinclient.common.model.ClientMeta;
import org.openthinclient.ldap.DirectoryException;

import java.util.Set;
import java.util.stream.Collectors;

public class DefaultLDAPClientMetaService extends AbstractLDAPService<ClientMeta> implements ClientMetaService {

  public DefaultLDAPClientMetaService(RealmService realmService) {
    super(ClientMeta.class, realmService);
  }

  @Override
  public Set<ClientMeta> findAll() {
    return withAllReams(realm -> {
      try {
        return realm.getDirectory().list(ClientMeta.class).stream();
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());
  }
}

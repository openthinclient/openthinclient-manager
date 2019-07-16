package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.FlatClient;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;

import java.util.Set;
import java.util.stream.Collectors;

public class DefaultLDAPFlatClientService extends AbstractLDAPService<FlatClient> implements FlatClientService {

  public DefaultLDAPFlatClientService(RealmService realmService) {
    super(FlatClient.class, realmService);
  }

  @Override
  public Set<FlatClient> findAll() {
    return withAllReams(realm -> {
      try {
        return realm.getDirectory().list(FlatClient.class).stream();
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());
  }
}

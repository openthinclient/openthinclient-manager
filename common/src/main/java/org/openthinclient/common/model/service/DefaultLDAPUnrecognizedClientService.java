package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;

import java.util.Set;
import java.util.stream.Collectors;

public class DefaultLDAPUnrecognizedClientService extends AbstractLDAPService<UnrecognizedClient> implements UnrecognizedClientService {

  public DefaultLDAPUnrecognizedClientService(RealmService realmService) {
    super(UnrecognizedClient.class, realmService);
  }

  @Override
  public Set<UnrecognizedClient> findByHwAddress(String hwAddressString) {
    return withAllReams(realm -> {
      try {
        return realm.getDirectory()
                .list(UnrecognizedClient.class,
                        new Filter("(&(macAddress={0})(!(l=*)))", hwAddressString),
                        TypeMapping.SearchScope.SUBTREE).stream();
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());

  }

  @Override
  public Set<UnrecognizedClient> findAll() {
    return withAllReams(realm -> {
      try {
        return realm.getDirectory().list(UnrecognizedClient.class).stream();
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());
  }

  @Override
  public UnrecognizedClient add(UnrecognizedClient unrecognizedClient) {

    final Realm realm = realmService.getDefaultRealm();

    try {
      realm.getDirectory().save(unrecognizedClient);
    } catch (DirectoryException e) {
      throw new RuntimeException(e);
    }

    return unrecognizedClient;
  }
}

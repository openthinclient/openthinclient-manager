package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultLDAPClientService implements ClientService {

  private final RealmService realmService;

  public DefaultLDAPClientService(RealmService realmService) {
    this.realmService = realmService;
  }

  @Override
  public Set<Client> findByHwAddress(String hwAddressString) {
    // TODO reading objects from the directory does not initialize the schema
    // we should think about whether schema initialization shall happen here or is in the responsibility of the caller

    return withAllReams(realm -> {
      try {
        return realm.getDirectory().list(Client.class,
                new Filter("(&(macAddress={0})(l=*))", hwAddressString),
                TypeMapping.SearchScope.SUBTREE).stream();
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());

  }

  @Override
  public Set<Client> findAll() {

    return withAllReams(realm -> {
      try {
        return realm.getDirectory().list(Client.class, null, TypeMapping.SearchScope.SUBTREE).stream();
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());

  }

  protected <T> Stream<T> withAllReams(Function<Realm, Stream<T>> function) {
    return realmService.findAllRealms().stream().flatMap(function);
  }
}

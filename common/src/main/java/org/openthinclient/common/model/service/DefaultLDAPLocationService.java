package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Location;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;

public class DefaultLDAPLocationService extends AbstractLDAPService<Location> implements LocationService {
  public DefaultLDAPLocationService(RealmService realmService) {
    super(Location.class, realmService);
  }

  @Override
  public Location findByName(String name) {
    return withAllReams(realm -> {
      try {
        return realm.getDirectory().list(Location.class,
                new Filter("(&(l={0}))", name),
                TypeMapping.SearchScope.SUBTREE)
                .stream();
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).findFirst().orElse(null);
  }
}

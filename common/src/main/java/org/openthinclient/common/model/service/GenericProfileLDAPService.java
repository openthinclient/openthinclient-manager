package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Profile;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;

import java.util.Set;
import java.util.stream.Collectors;

public class GenericProfileLDAPService extends AbstractLDAPService<Profile>  {

  public GenericProfileLDAPService(Class<Profile> type, RealmService realmService) {
    super(type, realmService);
  }

//  @Override
  public Set<String> findAllEntries(String name) {
    return withAllReams(realm -> {
      try {
        return realm.getDirectory().query(Profile.class,
            new Filter("(&(dn={0}))", name),
            null,
            TypeMapping.SearchScope.SUBTREE)
            .stream();
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());

  }
}

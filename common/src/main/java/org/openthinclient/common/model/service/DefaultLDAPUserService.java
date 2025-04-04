package org.openthinclient.common.model.service;

import java.util.Set;
import java.util.stream.Collectors;
import org.openthinclient.common.model.User;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultLDAPUserService extends AbstractLDAPService<User> implements UserService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLDAPUserService.class);

  public DefaultLDAPUserService(RealmService realmService) {
    super(User.class, realmService);
  }


  @Override
  public Set<User> findAll() {
    return super.findAll().stream().collect(Collectors.toSet());
  }

  @Override
  public User findByName(String name) {
    return withAllRealms((realm, dir) -> {
        String attr = "cn";
        if (realm.isSecondaryConfigured())
          attr = realm.getValue("Directory.Secondary.UserNameAttribute");

        return dir.list(User.class,
                        new Filter(String.format("(&(%s={0}))", attr), name),
                        TypeMapping.SearchScope.SUBTREE
                      ).stream();
        }).findFirst().orElse(null);
  }
}

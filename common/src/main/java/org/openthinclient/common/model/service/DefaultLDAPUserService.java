package org.openthinclient.common.model.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
  public Optional<User> findBySAMAccountName(String name) {
    return findByName(User.class, name).findFirst();
  }

}

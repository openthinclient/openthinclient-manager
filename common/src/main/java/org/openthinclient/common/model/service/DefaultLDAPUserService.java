package org.openthinclient.common.model.service;

import java.util.Set;
import java.util.stream.Collectors;
import org.openthinclient.common.model.User;
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
}

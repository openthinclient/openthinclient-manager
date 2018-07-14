package org.openthinclient.common.model.service;

import org.openthinclient.common.model.User;

public class DefaultLDAPUserService extends AbstractLDAPService<User> implements UserService {
  public DefaultLDAPUserService(RealmService realmService) {
    super(User.class, realmService);
  }
}

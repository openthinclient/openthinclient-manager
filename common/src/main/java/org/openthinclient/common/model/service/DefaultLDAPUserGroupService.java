package org.openthinclient.common.model.service;

import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;

public class DefaultLDAPUserGroupService extends AbstractLDAPService<UserGroup> implements UserGroupService {
  public DefaultLDAPUserGroupService(RealmService realmService) {
    super(UserGroup.class, realmService);
  }
}

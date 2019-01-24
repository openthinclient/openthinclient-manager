package org.openthinclient.common.model.service;

import org.openthinclient.common.model.ClientGroup;
import org.openthinclient.common.model.UserGroup;

public class DefaultLDAPClientGroupService extends AbstractLDAPService<ClientGroup> implements ClientGroupService {
  public DefaultLDAPClientGroupService(RealmService realmService) {
    super(ClientGroup.class, realmService);
  }
}

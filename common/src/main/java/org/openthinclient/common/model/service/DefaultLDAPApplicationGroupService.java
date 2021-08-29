package org.openthinclient.common.model.service;

import org.openthinclient.common.model.ApplicationGroup;

public class DefaultLDAPApplicationGroupService extends AbstractLDAPService<ApplicationGroup> implements ApplicationGroupService {
  public DefaultLDAPApplicationGroupService(RealmService realmService) {
    super(ApplicationGroup.class, realmService);
  }
}

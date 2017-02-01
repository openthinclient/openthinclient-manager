package org.openthinclient.common.model.service;

import org.openthinclient.common.model.HardwareType;

public class DefaultLDAPHardwareTypeService extends AbstractLDAPService<HardwareType> implements HardwareTypeService {
  public DefaultLDAPHardwareTypeService(RealmService realmService) {
    super(HardwareType.class, realmService);
  }

}

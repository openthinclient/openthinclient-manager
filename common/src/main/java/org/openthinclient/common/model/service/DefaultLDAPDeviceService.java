package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Device;

public class DefaultLDAPDeviceService extends AbstractLDAPService<Device> implements DeviceService {
  public DefaultLDAPDeviceService(RealmService realmService) {
    super(Device.class, realmService);
  }
}

package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;

public class DefaultLDAPDeviceService extends AbstractLDAPService<Device> implements DeviceService {
  public DefaultLDAPDeviceService(RealmService realmService) {
    super(Device.class, realmService);
  }

  @Override
  public void delete(Device device) throws DirectoryException {
    final Realm realm = device.getRealm();

    if (realm == null)
      throw new IllegalArgumentException("given device has no realm assigned.");

    realm.getDirectory().delete(device);
  }
}

package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Location;

public class DefaultLDAPLocationService extends AbstractLDAPService<Location> implements LocationService {
  public DefaultLDAPLocationService(RealmService realmService) {
    super(Location.class, realmService);
  }
}

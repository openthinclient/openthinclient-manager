package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Realm;

import java.util.Set;

public interface RealmService {

  /**
   * The default realm is by convention the first realm that will be encountered.
   */
  Realm getDefaultRealm();

  Set<Realm> findAllRealms();

  void reload();
}

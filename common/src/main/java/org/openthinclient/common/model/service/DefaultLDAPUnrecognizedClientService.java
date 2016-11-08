package org.openthinclient.common.model.service;

public class DefaultLDAPUnrecognizedClientService implements UnrecognizedClientService {

  private final RealmService realmService;

  public DefaultLDAPUnrecognizedClientService(RealmService realmService) {
    this.realmService = realmService;
  }
}

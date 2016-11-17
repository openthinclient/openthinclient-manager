package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Realm;

import java.util.function.Function;
import java.util.stream.Stream;

public class AbstractLDAPService {
  protected final RealmService realmService;

  public AbstractLDAPService(RealmService realmService) {
    this.realmService = realmService;
  }

  protected <T> Stream<T> withAllReams(Function<Realm, Stream<T>> function) {
    return realmService.findAllRealms().stream().flatMap(function);
  }
}

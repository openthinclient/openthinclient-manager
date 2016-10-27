package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Realm;

import java.util.Set;

public interface RealmService {
    Set<Realm> findAllRealms();
}

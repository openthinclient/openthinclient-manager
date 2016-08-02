package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;

import java.util.Set;

public interface ClientService {

    // FIXME get rid of the DirectoryExceptions. These have been handled by the surrounding code and should be replaced by something more meaningful.

    Set<Client> findByHwAddress(Realm realm, String hwAddressString) throws DirectoryException;

    Set<Client> findAll(Realm realm) throws DirectoryException;
}

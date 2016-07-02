package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;

import java.util.Set;

public class DefaultLDAPClientService implements ClientService {

    @Override
    public Set<Client> findByHwAddress(Realm realm, String hwAddressString) throws DirectoryException {
        // TODO reading objects from the directory does not initialize the schema
        // we should think about whether schema initialization shall happen here or is in the responsibility of the caller
        return realm.getDirectory().list(Client.class,
                new Filter("(&(macAddress={0})(l=*))", hwAddressString),
                TypeMapping.SearchScope.SUBTREE);
    }

    @Override
    public Set<Client> findAll(Realm realm) throws DirectoryException {

        return realm.getDirectory().list(Client.class, null, TypeMapping.SearchScope.SUBTREE);

    }
}

package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class DefaultLDAPClientService extends AbstractLDAPService implements ClientService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLDAPClientService.class);

  public DefaultLDAPClientService(RealmService realmService) {
    super(realmService);
  }

  @Override
  public Set<Client> findByHwAddress(String hwAddressString) {
    // TODO reading objects from the directory does not initialize the schema
    // we should think about whether schema initialization shall happen here or is in the responsibility of the caller

    return withAllReams(realm -> {
      try {
        return realm.getDirectory().list(Client.class,
                new Filter("(&(macAddress={0})(l=*))", hwAddressString),
                TypeMapping.SearchScope.SUBTREE)
                .stream() //
                .map(client -> initSchema(realm, client));
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());

  }

  private Client initSchema(Realm realm, Client client) {
    try {
      client.initSchemas(realm);
      return client;
    } catch (SchemaLoadingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Set<Client> findAll() {

    return withAllReams(realm -> {
      try {
        return realm.getDirectory().list(Client.class, null, TypeMapping.SearchScope.SUBTREE).stream()
                .map(client -> initSchema(realm, client));
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());

  }

  @Override
  public Client getDefaultClient() {

    final Set<Client> res = findByHwAddress(DEFAULT_CLIENT_MAC);

    if (res.size() > 1) {
      // FIXME we should have some kind of generic "there is something wrong, check it" mechanism to notify the user
      LOGGER.error("More than one default client configuration found. This is likely to cause problems.");
    }

    if (res.size() > 0) {
      return res.iterator().next();
    }
    return null;
  }

}

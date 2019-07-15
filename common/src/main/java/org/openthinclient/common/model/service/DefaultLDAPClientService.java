package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.ClientMeta;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultLDAPClientService extends AbstractLDAPService<Client> implements ClientService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLDAPClientService.class);

  public DefaultLDAPClientService(RealmService realmService) {
    super(Client.class, realmService);
  }

  @Override
  public Set<Client> findByHwAddress(String hwAddressString) {
    return withAllReams(realm -> {
      try {
        return realm.getDirectory().list(Client.class,
                new Filter("(&(macAddress={0})(l=*))", hwAddressString),
                TypeMapping.SearchScope.SUBTREE)
                .stream() //
                .map(this::initSchema);
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());

  }

  private Client initSchema(Client client) {
    try {
      client.initSchemas(client.getRealm());
      return client;
    } catch (SchemaLoadingException e) {
      throw new RuntimeException(e);
    }
  }

  private ClientMeta initSchema(ClientMeta client) {
    try {
      client.initSchemas(client.getRealm());
      return client;
    } catch (SchemaLoadingException e) {
      throw new RuntimeException(e);
    }
  }

  public Set<ClientMeta> findAllClientMeta() {
    return withAllReams(realm -> {
      try {
        return realm.getDirectory().list(ClientMeta.class,
            new Filter("(objectClass=ipHost)"),
             "ou=clients,ou=openthinclient,dc=openthinclient,dc=org",
            TypeMapping.SearchScope.SUBTREE)
            .stream();
//        .map(this::initSchema);
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());
  }


//  @Override
//  public Set<ClientMeta> findAllClientMeta() {
//    return withAllReams(realm -> {
//      try {
//        return realm.getDirectory().list(ClientMeta.class).stream();
//      } catch (DirectoryException e) {
//        throw new RuntimeException(e);
//      }
//    }).collect(Collectors.toSet());
//  }

  @Override
  public Set<Client> findAll() {
    return super.findAll().stream().map(this::initSchema).collect(Collectors.toSet());
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

  @Override
  public Client findByName(final String name) {
    return findByName(Client.class, name).map(this::initSchema).findFirst().orElse(null);
  }

  @Override
  public void save(Client client) {
    try {
      realmService.getDefaultRealm().getDirectory().save(client);
    } catch (DirectoryException e) {
      throw translateException(e);
    }
  }

}

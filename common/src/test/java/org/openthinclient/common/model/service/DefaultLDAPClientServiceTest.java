package org.openthinclient.common.model.service;

import org.junit.Before;
import org.junit.Test;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.AbstractSchemaProvider;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.common.test.ldap.AbstractEmbeddedDirectoryTest;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class DefaultLDAPClientServiceTest extends AbstractEmbeddedDirectoryTest {

  private DefaultLDAPClientService clientService;
  private DefaultLDAPRealmService realmService;
  private LDAPDirectory ldapDirectory;

  @Before
  public void setupServices() throws Exception {

    final LDAPConnectionDescriptor lcd = prepareEnvironment(baseDN, "TestRealm");

    this.ldapDirectory = LDAPDirectory.openEnv(lcd);
    final Realm realm = initRealm(this.ldapDirectory, "Test Realm");
    initOUs(ldapDirectory);

    realm.setConnectionDescriptor(lcd);


    realmService = new DefaultLDAPRealmService(new AbstractSchemaProvider() {
      @Override
      protected List<Schema> loadDefaultSchema(String profileTypeName) throws SchemaLoadingException {
        final Schema schema = loadSchema(DefaultLDAPClientServiceTest.this.getClass().getResourceAsStream("/schemas/tcos-devices/schema/" + profileTypeName + ".xml"));
        return singletonList(schema);
      }

      @Override
      protected List<Schema> loadAllSchemas(String profileTypeName) throws SchemaLoadingException {
        return emptyList();
      }
    }, getConnectionDescriptor());
    clientService = new DefaultLDAPClientService(realmService);

  }

  @Test
  public void testCreateClient() throws Exception {

    final Client client = new Client();
    client.setName("My Simple Client");

    clientService.save(client);

    final Client client2 = new Client();
    client2.setName("Another Simple Client");
    clientService.save(client2);

    final Set<Client> clients = clientService.findAll();

    assertEquals(2, clients.size());

  }

  @Test
  public void testFindByName() throws Exception {
    final Client client = new Client();
    client.setName("My Simple Client");

    clientService.save(client);

    final Client client2 = new Client();
    client2.setName("Another Simple Client");
    clientService.save(client2);

    final Client result = clientService.findByName("My Simple Client");

    assertNotNull(result);
    assertEquals("My Simple Client", result.getName());
    assertNotNull(result.getDn());

  }
}
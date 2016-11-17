package org.openthinclient.common.model.service;

import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

public class DefaultLDAPRealmService implements RealmService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLDAPRealmService.class);
  private final SchemaProvider schemaProvider;
  private volatile Set<Realm> realms;

  public DefaultLDAPRealmService(SchemaProvider schemaProvider) {
    this.schemaProvider = schemaProvider;
  }

  @Override
  public Realm getDefaultRealm() {

    final Set<Realm> all = findAllRealms();

    if (all.size() > 0)
      return all.iterator().next();
    return null;
  }

  @Override
  public Set<Realm> findAllRealms() {
    if (realms == null) {
      synchronized (this) {
        if (realms == null) {
          reload();
        }
      }
    }
    return Collections.unmodifiableSet(realms);
  }

  @Override
  public void reload() {
    synchronized (this) {
      LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
      lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
      lcd.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);

      // FIXME read from configuration
      // FIXME read from configuration
      // FIXME read from configuration
      // FIXME read from configuration
      // FIXME read from configuration
      // FIXME read from configuration

      lcd.setCallbackHandler(new UsernamePasswordHandler("uid=admin,ou=system",
              System.getProperty("ContextSecurityCredentials", "secret")
                      .toCharArray()));
      try {
        final Set<Realm> realms = LDAPDirectory.findAllRealms(lcd);
        for (Realm realm : realms) {
          realm.setSchemaProvider(schemaProvider);
          try {
            final Schema schema = schemaProvider.getSchema(Realm.class, null);
            realm.setSchema(schema);
          } catch (SchemaLoadingException e) {
            LOGGER.error("Failed to load realm schema definition. Trying to proceed.", e);
          }
        }
        this.realms = realms;
      } catch (DirectoryException e) {
        // FIXME better exception handling
        throw new RuntimeException(e);
      }
    }
  }
}

package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.ldap.DirectoryException;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultLDAPApplicationService extends AbstractLDAPService implements ApplicationService {


  public DefaultLDAPApplicationService(RealmService realmService) {
    super(realmService);
  }

  @Override
  public Set<Application> findAll() {

    return findAllAsStream().collect(Collectors.toSet());
  }

  private Stream<Application> findAllAsStream() {
    return withAllReams((realm) -> {
      try {
        return realm.getDirectory().list(Application.class).stream();
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public void delete(Application application) {

    final Realm realm = application.getRealm();

    if (realm == null)
      throw new IllegalArgumentException("given application has no realm assigned.");

    try {
      realm.getDirectory().delete(application);
    } catch (DirectoryException e) {
      throw new RuntimeException("Failed to delete application object", e);
    }

  }

  @Override
  public Set<Application> findAllUsingSchema(Schema<?> schema) {

    final String schemaName = schema.getName();

    return findAllAsStream() //
            .filter(application -> schemaName.equalsIgnoreCase(getSchemaName(application))) //
            .collect(Collectors.toSet());
  }

  private String getSchemaName(Application application) {
    final Realm realm = application.getRealm();
    final Schema schema = application.getSchema(realm);
    return schema.getName();
  }
}

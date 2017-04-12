package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.ldap.DirectoryException;

import java.util.Set;
import java.util.stream.Collectors;

public class DefaultLDAPApplicationService extends AbstractLDAPService<Application> implements ApplicationService {


  public DefaultLDAPApplicationService(RealmService realmService) {
    super(Application.class, realmService);
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

    return findAll(Application.class) //
            .filter(application -> schemaName.equalsIgnoreCase(getSchemaName(application))) //
            .collect(Collectors.toSet());
  }

  private String getSchemaName(Application application) {
    final Realm realm = application.getRealm();
    final Schema schema = application.getSchema(realm);
    return schema.getName();
  }
}

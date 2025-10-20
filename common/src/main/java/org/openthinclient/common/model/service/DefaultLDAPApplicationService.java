package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.ldap.DirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class DefaultLDAPApplicationService extends AbstractLDAPService<Application> implements ApplicationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLDAPApplicationService.class);

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
  public Set<Application> findAllUsingSchema(Schema schema) {
    long start = System.currentTimeMillis();
    final String schemaName = schema.getName();
    Set<Application> collect = findAll(Application.class) //
        .filter(application -> schemaName.equalsIgnoreCase(getSchemaName(application))) //
        .collect(Collectors.toSet());
    LOGGER.info("FindAll Application took " + (System.currentTimeMillis() - start) + "ms");
    return collect;

  }

  private String getSchemaName(Application application) {
    final Realm realm = application.getRealm();
    try {
      return application.getSchema(realm).getName();
    } catch (SchemaLoadingException ex) {
      LOGGER.error("No schema for {}",
                   application.getSchemaName2());
      return null;
    }
  }
}

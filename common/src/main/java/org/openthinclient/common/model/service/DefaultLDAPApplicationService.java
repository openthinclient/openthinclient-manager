package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultLDAPApplicationService extends AbstractLDAPService<Application> implements ApplicationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLDAPApplicationService.class);
  private static final String REPLACEMENT = "\\\\$0";
  private static final Pattern QUOTE_TO_LDAP = Pattern.compile("[\\\\,=()]");

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
    long start = System.currentTimeMillis();
    final String schemaName = schema.getName();
    Set<Application> collect = findAll(Application.class) //
        .filter(application -> schemaName.equalsIgnoreCase(getSchemaName(application))) //
        .collect(Collectors.toSet());
    LOGGER.info("FindAll Application took " + (System.currentTimeMillis() - start) + "ms");
    return collect;

  }

  @Override
  public Set<Application> findByUniqueMember(final String dn) {
    return withAllRealms((r, dir) -> {
      return dir.list(Application.class,
          new Filter("(&(uniquemember={0}))", dn),
          TypeMapping.SearchScope.SUBTREE)
          .stream();
    }).collect(Collectors.toSet());
  }

  private String getSchemaName(Application application) {
    final Realm realm = application.getRealm();
    final Schema schema = application.getSchema(realm);
    return schema.getName();
  }
}

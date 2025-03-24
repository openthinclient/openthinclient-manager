package org.openthinclient.common.model.service;

import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbstractLDAPService<T extends DirectoryObject> implements DirectoryObjectService<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLDAPService.class);
  protected final RealmService realmService;
  private final Class<T> type;

  public AbstractLDAPService(Class<T> type, RealmService realmService) {
    this.type = type;
    this.realmService = realmService;
  }

  protected <O extends DirectoryObject> Stream<O> findAll(final Class<O> type) {
    return withAllReams(realm -> {
      try {
        return realm.getDirectory().list(type, null, TypeMapping.SearchScope.SUBTREE).stream();
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    });
  }

  protected <O extends DirectoryObject> Stream<O> findByName(final Class<O> type, final String name) {
    return withAllRealms((r, dir) -> {
      return dir.list(type,
              new Filter("(&(cn={0}))", name),
              TypeMapping.SearchScope.SUBTREE)
              .stream();
    });
  }

  @Override
  public Set<T> findAll() {
    long start = System.currentTimeMillis();
    Set<T> collect = findAll(type).collect(Collectors.toSet());
    LOGGER.debug("FindAll " + type.getSimpleName()  + " took " + (System.currentTimeMillis() - start) + "ms");
    return collect;
  }

  @Override
  public T findByName(String name) {
    return findByName(type, name).findFirst().orElse(null);
  }

  @Override
  public void save(T object) {
    final LDAPDirectory directory;
    try {
      if (object.getRealm() != null && object.getRealm().getDirectory() != null) {
        directory = object.getRealm().getDirectory();
      } else {
        directory = realmService.getDefaultRealm().getDirectory();
      }

      directory.save(object);
    } catch (DirectoryException e) {
      throw translateException(e);
    }
  }

  protected <T> Stream<T> withAllRealms(RealmCallback<T> callback) {
    return withAllReams((r) -> {
      try {
        return callback.execute(r, r.getDirectory());
      } catch (DirectoryException e) {

        throw translateException(e);
      }
    });
  }

  protected RuntimeException translateException(DirectoryException e) {
    // FIXME better exception translation.
    return new RuntimeException("Failed to access directory", e);
  }

  protected <T> Stream<T> withAllReams(Function<Realm, Stream<T>> function) {
    return realmService.findAllRealms().stream().flatMap(function);
  }

  @FunctionalInterface
  public interface RealmCallback<T> {
    Stream<T> execute(Realm realm, LDAPDirectory directory) throws DirectoryException;
  }

  public void reloadAllSchemas() {
    realmService.getDefaultRealm().getSchemaProvider().reload();
  }

  @Override
  public int count() {
    return queryNames().size();
  }

  @Override
  public Set<String> queryNames() {
    long start = System.currentTimeMillis();
    Set<String> names = withAllReams(realm -> {
      try {
        return realm.getDirectory().query(type, null, null, TypeMapping.SearchScope.SUBTREE).stream();
      } catch (DirectoryException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet());
    LOGGER.debug(type.getSimpleName() + "-queryNames took " + (System.currentTimeMillis() - start) + "ms");
    return names;
  }
}

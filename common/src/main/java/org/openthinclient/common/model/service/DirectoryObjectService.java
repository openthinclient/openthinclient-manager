package org.openthinclient.common.model.service;

import org.openthinclient.common.model.DirectoryObject;

import java.util.Set;

/**
 * A common base interface for all services pertaining to {@link DirectoryObject directory objects}. This interface only
 * defines common methods for interaction with {@link DirectoryObject}
 * objects that all services require to provide.
 */
public interface DirectoryObjectService<T extends DirectoryObject> {

  Set<T> findAll();

  T findByName(String name);

  void save(T object);

  /** Reloads all schemas */
  void reloadAllSchemas();

  int count();
}

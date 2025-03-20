package org.openthinclient.common.model.service;

import org.openthinclient.common.model.User;

import java.util.Set;

public interface UserService extends DirectoryObjectService<User> {

  Set<User> findAll();

}

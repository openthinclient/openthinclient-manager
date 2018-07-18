package org.openthinclient.common.model.service;

import org.openthinclient.common.model.User;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface UserService extends DirectoryObjectService<User> {


  Optional<User> findBySAMAccountName(String sAMAccountName);

  Set<User> findAll();

}

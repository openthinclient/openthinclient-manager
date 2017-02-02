package org.openthinclient.common.model.service;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.schema.Schema;

import java.util.Set;

public interface ApplicationService {

  /**
   * Find all application definitions.
   */
  Set<Application> findAll();

  /**
   * Delete the specified {@link Application} from the configuration. This operation will also get
   * rid of any references to the {@link Application}
   */
  void delete(Application application);

  /**
   * Search for all applications using the provided schema.
   *
   * @param schema the {@link Schema} for which applications should be searched for
   * @return a {@link java.util.Set} of {@link Application} using the specified {@link Schema}
   */
  Set<Application> findAllUsingSchema(Schema<?> schema);

}

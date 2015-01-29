package org.openthinclient.service.common.home;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationPath {

  /**
   *
   * @return
   */
  String value();

}

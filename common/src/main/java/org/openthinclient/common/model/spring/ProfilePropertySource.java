package org.openthinclient.common.model.spring;

import org.openthinclient.common.model.Profile;
import org.springframework.core.env.PropertySource;

/**
 * A {@link PropertySource} wrapping openthinclient {@link Profile} instances. <br/> The {@link
 * ProfilePropertySource} will provide values from {@link Profile#getValue(String)} as resolvable
 * properties.
 */
public final class ProfilePropertySource<T extends Profile> extends PropertySource<T> {

  public ProfilePropertySource(T source) {
    super("profile/" + source.getName(), source);
  }

  @Override
  public Object getProperty(String name) {
    return getSource().getValue(name);
  }
}

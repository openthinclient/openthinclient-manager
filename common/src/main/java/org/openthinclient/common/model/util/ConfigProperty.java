package org.openthinclient.common.model.util;

import org.openthinclient.common.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertySource;

public interface ConfigProperty<T> {

  String getPropertyName();

  T get(Profile profile);

  T get(PropertySource<?> propertySource);

  class StringConfig implements ConfigProperty<String> {
    private final String propertyName;

    public StringConfig(String propertyName) {
      this.propertyName = propertyName;
    }

    @Override
    public String getPropertyName() {
      return propertyName;
    }

    @Override
    public String get(Profile profile) {
      return null;
    }

    @Override
    public String get(PropertySource<?> propertySource) {
      return null;
    }
  }

  class EnumConfig<T extends Enum<T>> implements ConfigProperty<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnumConfig.class);

    private final String propertyName;
    private final Class<T> enumClass;
    private final T defaultValue;

    public EnumConfig(String propertyName, Class<T> enumClass, T defaultValue) {
      this.propertyName = propertyName;
      this.enumClass = enumClass;
      this.defaultValue = defaultValue;
    }

    @Override
    public String getPropertyName() {
      return propertyName;
    }

    @Override
    public T get(Profile profile) {
      return decode(profile.getValue(propertyName));
    }

    private T decode(Object value) {
      if (value == null)
        return defaultValue;

      if (enumClass.isAssignableFrom(value.getClass())) {
        return (T) value;
      }

      String s;
      if (!(value instanceof String)) {
        s = value.toString();
      } else {
        s = (String) value;
      }

      try {
        return Enum.valueOf(enumClass, s);
      } catch (Exception e) {
        LOGGER.error("Incorrect value specified for property " + propertyName + ". Value: " + s);
        return defaultValue;
      }

    }

    @Override
    public T get(PropertySource<?> propertySource) {
      return decode(propertySource.getProperty(propertyName));
    }
  }

}

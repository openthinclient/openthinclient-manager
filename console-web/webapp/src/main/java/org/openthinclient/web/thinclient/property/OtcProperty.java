package org.openthinclient.web.thinclient.property;

import org.openthinclient.web.thinclient.model.ItemConfiguration;

/**
 *
 */
public abstract class OtcProperty {

  private final String label;
  private final String key;
  private final String defaultValue;

  public OtcProperty(String label, String key, String defaultValue) {
    this.label = label;
    this.key = key;
    this.defaultValue = defaultValue;
  }

  public abstract void setConfiguration(ItemConfiguration bean);

  public abstract ItemConfiguration getConfiguration();

  public String getLabel() {
    return label;
  }

  public String getKey() {
    return key;
  }

  /**
   * Return default value, may be null
   * @return default value, may be null
   */
  public String getDefaultValue() {
    return defaultValue;
  }
}

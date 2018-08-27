package org.openthinclient.web.thinclient.property;

import org.openthinclient.web.thinclient.model.ItemConfiguration;

/**
 *
 */
public abstract class OtcProperty {

  private final String label;
  private final String key;
  private final String defaultValue;
  private final String tip;

  public OtcProperty(String label, String tip, String key, String defaultValue) {
    this.label = label;
    this.key = key;
    this.defaultValue = defaultValue;
    this.tip = tip;
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

  public String getTip() {
    return tip;
  }
}

package org.openthinclient.web.thinclient.property;

import org.openthinclient.web.thinclient.model.ItemConfiguration;

/**
 *
 */
public abstract class OtcProperty {

  private final String label;
  private final String key;
  private final String initialValue;
  private final String tip;

  public OtcProperty(String label, String tip, String key, String initialValue) {
    this.label = label;
    this.key = key;
    this.initialValue = initialValue;
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
   * Return initial value, may be null
   * @return initial value, may be null
   */
  public String getInitialValue() {
    return initialValue;
  }

  public String getTip() {
    return tip;
  }
}

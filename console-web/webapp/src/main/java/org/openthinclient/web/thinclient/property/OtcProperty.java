package org.openthinclient.web.thinclient.property;

import org.openthinclient.web.thinclient.model.ItemConfiguration;

/**
 *
 */
public abstract class OtcProperty {

  private final String label;
  private final String key;

  public OtcProperty(String label, String key) {
    this.label = label;
    this.key = key;
  }

  public abstract void setConfiguration(ItemConfiguration bean);

  public abstract ItemConfiguration getConfiguration();

  public String getLabel() {
    return label;
  }

  public String getKey() {
    return key;
  }
}

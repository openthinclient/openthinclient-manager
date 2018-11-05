package org.openthinclient.web.thinclient.property;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.web.thinclient.model.ItemConfiguration;

/**
 *
 */
public class OtcTextProperty extends OtcProperty {

  private ItemConfiguration config;

  public OtcTextProperty(String label, String tip, String key, String initialValue) {
    super(label, tip, key, initialValue);
  }

  public OtcTextProperty(String label,  String tip, String key, String value, String initialValue) {
    super(label, tip, key, initialValue);
    config = new ItemConfiguration(key, value);
  }

  @Override
  public void setConfiguration(ItemConfiguration configuration) {
    this.config = configuration;
  }

  @Override
  public ItemConfiguration getConfiguration() {
    return config;
  }

  public String getValue() {
    return config.getValue();
  }

  public void setValue(String value) {
    this.config.setValue(value);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("label", getLabel())
        .append("key", getKey())
        .append("initialValue", getInitialValue())
        .append("configuration.value", getValue())
        .toString();
  }
}

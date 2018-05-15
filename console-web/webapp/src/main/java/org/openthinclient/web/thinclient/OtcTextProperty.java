package org.openthinclient.web.thinclient;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.web.thinclient.model.ItemConfiguration;

/**
 *
 */
public class OtcTextProperty extends OtcProperty {

  private ItemConfiguration config;

  public OtcTextProperty(String label, String key) {
    super(label, key);
  }

  @Override
  public void setBean(ItemConfiguration bean) {
    this.config = bean;
  }

  @Override
  public ItemConfiguration getBean() {
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
        .append("value", getValue())
        .toString();
  }
}

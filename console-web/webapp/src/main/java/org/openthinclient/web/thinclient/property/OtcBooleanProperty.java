package org.openthinclient.web.thinclient.property;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.web.thinclient.model.ItemConfiguration;

/**
 *
 */
public class OtcBooleanProperty extends OtcProperty {

  private ItemConfiguration config;

  public OtcBooleanProperty(String label, String key) {
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

  public boolean isValue() {
    return Boolean.valueOf(config.getValue());
  }

  public void setValue(boolean value) {
    this.config.setValue(String.valueOf(value));
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("label", getLabel())
        .append("key", getKey())
        .append("value", isValue())
        .toString();
  }
}

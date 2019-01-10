package org.openthinclient.web.thinclient.property;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.web.thinclient.model.ItemConfiguration;

/**
 *
 */
public class OtcBooleanProperty extends OtcProperty {

  private ItemConfiguration config;
  private String valueOfTrue;
  private String valueOfFalse;

  public OtcBooleanProperty(String label, String tip, String key, String initialValue, String valueOfTrue, String valueOfFalse) {
    super(label, tip, key, initialValue);
    this.valueOfTrue  = valueOfTrue;
    this.valueOfFalse = valueOfFalse;
  }

  @Override
  public void setConfiguration(ItemConfiguration bean) {
    this.config = bean;
  }

  @Override
  public ItemConfiguration getConfiguration() {
    return config;
  }

  public boolean isValue() {
    if (config.getValue() == null || config.getValue().length() == 0) { // use default value
      return getInitialValue().equals(valueOfTrue);
    } else {
      return config.getValue().equals(valueOfTrue);
    }
  }

  public void setValue(boolean value) {
    this.config.setValue(value ? valueOfTrue : valueOfFalse);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("label", getLabel())
        .append("key", getKey())
        .append("initialValue", getInitialValue())
        .append("value", isValue())
        .toString();
  }

}
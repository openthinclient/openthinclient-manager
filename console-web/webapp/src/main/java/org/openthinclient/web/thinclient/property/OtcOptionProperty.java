package org.openthinclient.web.thinclient.property;

import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.web.thinclient.model.ItemConfiguration;

/**
 *
 */
public class OtcOptionProperty extends OtcProperty {

  private ItemConfiguration config;
  private List<String> options;

  public OtcOptionProperty(String label, String key, List<String> options) {
    super(label, key);
    this.options = options;
  }

  @Override
  public void setConfiguration(ItemConfiguration bean) {
    this.config = bean;
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

  public List<String> getOptions() {
    return options;
  }

  public void setOptions(List<String> options) {
    this.options = options;
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

package org.openthinclient.web.thinclient.property;

import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;

/**
 *
 */
public class OtcOptionProperty extends OtcProperty {

  private ItemConfiguration config;
  private List<SelectOption> options;

  /**
   *
   * @param label display Label of property
   * @param key the key of property
   * @param options possible values
   */
  public OtcOptionProperty(String label, String key, String defaultValue, List<SelectOption> options) {
    super(label, key, defaultValue);
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

  public List<SelectOption> getOptions() {
    return options;
  }

  public void setOptions(List<SelectOption> options) {
    this.options = options;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("label", getLabel())
        .append("key", getKey())
        .append("defaultValue", getDefaultValue())
        .append("value", getValue())
        .toString();
  }

  public SelectOption getSelectOption(String val) {

    String value;
    if (val == null || val.length() == 0) {
      value = getDefaultValue();
    } else {
      value = val;
    }

    return options.stream().filter(selectOption -> selectOption.getValue().equals(value)).findFirst().orElse(null);
  }
}

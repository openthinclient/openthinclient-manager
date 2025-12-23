package org.openthinclient.web.thinclient.property;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.web.thinclient.model.ItemConfiguration;

import java.util.regex.Pattern;

public class OtcTextProperty extends OtcProperty {

  /* Unicode format control characters, especially Bidi controls */
  private final static Pattern UNICODE_FORMAT_CONTROL = Pattern.compile(
      "[\\p{Cf}]");

  private ItemConfiguration config;

  public OtcTextProperty(String label, String tip, String key, String initialValue, String defaultSchemaValue) {
    super(label, tip, key, initialValue, defaultSchemaValue);
  }

  public OtcTextProperty(String label,  String tip, String key, String value, String initialValue, String defaultSchemaValue) {
    super(label, tip, key, initialValue, defaultSchemaValue);
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
    this.config.setValue(UNICODE_FORMAT_CONTROL.matcher(value).replaceAll(""));
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("label", getLabel())
        .append("key", getKey())
        .append("value", getValue())
        .append("initialValue", getInitialValue())
        .append("defaultSchemaValue", getDefaultSchemaValue())
        .toString();
  }
}

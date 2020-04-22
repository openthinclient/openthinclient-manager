package org.openthinclient.web.thinclient.property;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;

/**
 * OtcBoolean-property type
 * - initial unset values (null) will be displayed by schema-default-value: null -> true|false
 * - if initial value was unset (null) AND is not changed by UI, it will be set to null value: true|false -> null
 * - an already set value (true|false) will be handled as true or false
 */
public class OtcBooleanProperty extends OtcProperty {

  private ItemConfiguration config;
  private String valueOfTrue;
  private String valueOfFalse;
  private String labelOfTrue;
  private String labelOfFalse;
  private final static String JUST_ONE_WORD = "\\s*\\S+\\s*";

  public OtcBooleanProperty(String label, String tip, String key, String initialValue, Boolean defaultSchemaValue, SelectOption falseOption, SelectOption trueOption) {
    super(label, tip, key, initialValue, defaultSchemaValue.toString());
    this.valueOfTrue  = trueOption.getValue();
    this.valueOfFalse = falseOption.getValue();

    this.labelOfTrue = trueOption.getLabel();
    if(labelOfTrue.matches(JUST_ONE_WORD)) {
      labelOfTrue = null;
    }
    this.labelOfFalse = falseOption.getLabel();
    if(labelOfFalse.matches(JUST_ONE_WORD)) {
      labelOfFalse = null;
    }
  }

  @Override
  public void setConfiguration(ItemConfiguration bean) {
    this.config = bean;
  }

  @Override
  public ItemConfiguration getConfiguration() {
    return config;
  }

  public Boolean isValue() {
    if (config.getValue() == null || config.getValue().length() == 0) { // handle un-set value
      return getInitialValue() == null ? Boolean.valueOf(getDefaultSchemaValue()) : getInitialValue().equals(valueOfTrue);
    } else {
      return config.getValue().equals(valueOfTrue);
    }
  }

  public void setValue(Boolean value) {
    if (value == null || value.equals(Boolean.valueOf(getDefaultSchemaValue()))) {
      this.config.setValue(null);
    } else {
      this.config.setValue(value ? valueOfTrue : valueOfFalse);
    }
  }

  public String getLabelFor(Boolean value) {
    return value != null && value ? labelOfTrue : labelOfFalse;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("label", getLabel())
        .append("key", getKey())
        .append("value", isValue())
        .append("initialValue", getInitialValue())
        .append("defaultSchemaValue", getDefaultSchemaValue())
        .toString();
  }

}

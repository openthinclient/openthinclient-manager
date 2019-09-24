package org.openthinclient.web.thinclient.property;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;

/**
 *
 */
public class OtcBooleanProperty extends OtcProperty {

  private ItemConfiguration config;
  private String valueOfTrue;
  private String valueOfFalse;
  private String labelOfTrue;
  private String labelOfFalse;
  private final static String JUST_ONE_WORD = "\\s*\\S+\\s*";

  public OtcBooleanProperty(String label, String tip, String key, String initialValue, SelectOption falseOption, SelectOption trueOption) {
    super(label, tip, key, initialValue);
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

  public String getLabelFor(boolean value) {
    return value? labelOfTrue : labelOfFalse;
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

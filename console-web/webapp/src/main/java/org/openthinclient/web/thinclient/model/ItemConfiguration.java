package org.openthinclient.web.thinclient.model;

import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.Validator;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Konfiguration für ein Property(-Item)
 *
 * - Validatoren
 * - Converter
 */
public class ItemConfiguration {

  private Item item;
  private String key;
  private String value;
  private String type;
  private boolean required = false;
  private boolean disabled = false;

  private List<Validator> validators = new ArrayList<>();

  public ItemConfiguration(String key, String value) {
    this.key = key;
    this.value = value;
    this.type = String.class.getName();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("k", key)
            .append("v", value)
            .append("t", type)
            .toString();
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }


  public List<Validator> getValidators() {
    return validators;
  }

  public void addValidator(Validator validator) {
    this.validators.add(validator);
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public boolean isRequired() {
    return required;
  }

  public void disable() {
    disabled = true;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public void enable() {
    disabled = false;
  }
}

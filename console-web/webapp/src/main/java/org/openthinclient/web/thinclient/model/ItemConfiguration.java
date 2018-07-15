package org.openthinclient.web.thinclient.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Konfiguration für ein Property(-Item), könnte enthalten:
 *
 * - i18n Texte
 * - default Werte
 * - Validatoren
 */
public class ItemConfiguration {

  private Item item;
  private String key;
  private String value;
  private String type;

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
}

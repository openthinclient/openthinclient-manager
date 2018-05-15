package org.openthinclient.web.thinclient.model;

/**
 *
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

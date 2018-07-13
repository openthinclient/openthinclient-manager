package org.openthinclient.web.thinclient.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Item {

  private String name;
  private String description;
  private Type type;

  private List<ItemConfiguration> configuration = new ArrayList<>();

  public Item(String name, Type type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public List<ItemConfiguration> getConfiguration() {
    return configuration;
  }

  public void setConfiguration(List<ItemConfiguration> configuration) {
    this.configuration = configuration;
  }

  public void addConfig(ItemConfiguration configuration) {
    configuration.setItem(this);
    this.configuration.add(configuration);
  }

  public ItemConfiguration getConfiguration(String key) {
    return this.configuration.stream().filter(ic -> ic.getKey().equals(key)).findAny().orElse(new ItemConfiguration(key, null));
  }


  public enum Type {
    DEVICE,
    APPLICATION,
    HARDWARE,
    LOCATION,
    CLIENT;
  }
}

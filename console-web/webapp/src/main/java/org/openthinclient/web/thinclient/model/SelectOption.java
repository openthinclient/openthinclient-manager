package org.openthinclient.web.thinclient.model;

public class SelectOption {

  String label;
  String value;

  public SelectOption() { }

  public SelectOption(String label, String value) {
    this.label = label;
    this.value = value;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

}

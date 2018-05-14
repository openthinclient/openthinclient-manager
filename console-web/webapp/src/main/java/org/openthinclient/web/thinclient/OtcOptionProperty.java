package org.openthinclient.web.thinclient;

import java.util.List;

/**
 *
 */
public class OtcOptionProperty extends OtcProperty {

  private String value;
  private List<String> options;

  public OtcOptionProperty(String label, String value, List<String> options) {
    super(label);
    this.value = value;
    this.options = options;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public List<String> getOptions() {
    return options;
  }

  public void setOptions(List<String> options) {
    this.options = options;
  }
}

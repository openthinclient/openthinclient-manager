package org.openthinclient.web.thinclient;

/**
 *
 */
public class OtcTextProperty extends OtcProperty {

  private String value;

  public OtcTextProperty(String label, String value) {
    super(label);
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}

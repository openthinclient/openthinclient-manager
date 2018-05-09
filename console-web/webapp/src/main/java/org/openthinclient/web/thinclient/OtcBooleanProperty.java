package org.openthinclient.web.thinclient;

/**
 *
 */
public class OtcBooleanProperty extends OtcProperty {

  private boolean value;

  public OtcBooleanProperty(String label, boolean value) {
    super(label);
    this.value = value;
  }

  public boolean isValue() {
    return value;
  }

  public void setValue(boolean value) {
    this.value = value;
  }
}

package org.openthinclient.web.thinclient.property;

/**
 * OtcPasswordProperty special case of OtcTextProperty
 */
public class OtcPasswordProperty extends OtcTextProperty {

  public OtcPasswordProperty(String label, String tip, String key, String initialValue) {
    super(label, tip, key, initialValue);
  }

  public OtcPasswordProperty(String label, String tip, String key, String value, String initialValue) {
    super(label, tip, key, value, initialValue);
  }
}

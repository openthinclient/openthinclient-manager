package org.openthinclient.web.thinclient.property;

/**
 * OtcPasswordProperty special case of OtcTextProperty
 */
public class OtcPasswordProperty extends OtcTextProperty {

  private boolean hashed = false;

  public OtcPasswordProperty(String label, String tip, String key, String initialValue) {
    this(label, tip, key, initialValue, false);
  }

  public OtcPasswordProperty(String label, String tip, String key,
                             String initialValue, boolean hashed) {
    super(label, tip, key, initialValue, null);
    this.hashed = hashed;
  }

  public boolean isHashed() {
    return hashed;
  }

}

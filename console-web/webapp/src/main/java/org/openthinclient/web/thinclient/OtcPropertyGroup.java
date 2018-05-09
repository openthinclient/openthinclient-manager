package org.openthinclient.web.thinclient;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class OtcPropertyGroup {

  private final String label;
  private final List<OtcProperty> otcProperties;

  public OtcPropertyGroup(String label, OtcProperty... otcProperties) {
    this.label = label;
    if (otcProperties != null) {
      this.otcProperties = Arrays.asList(otcProperties);
    } else {
      this.otcProperties = new ArrayList<>();
    }
  }

  public List<OtcProperty> getOtcProperties() {
    return otcProperties;
  }

  public String getLabel() {
    return label;
  }
}

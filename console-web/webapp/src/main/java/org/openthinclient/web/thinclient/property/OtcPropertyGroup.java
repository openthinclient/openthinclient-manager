package org.openthinclient.web.thinclient.property;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class OtcPropertyGroup {

  private final String label;
  private List<OtcProperty> otcProperties = new ArrayList<>();
  private List<OtcPropertyGroup> groups = new ArrayList<>();

  public OtcPropertyGroup(String label, OtcProperty... otcProperties) {
    this.label = label;
    if (otcProperties != null) {
      Arrays.asList(otcProperties).forEach(o -> this.otcProperties.add((OtcProperty) o));
    }
  }

  public List<OtcProperty> getOtcProperties() {
    return otcProperties;
  }

  public String getLabel() {
    return label;
  }

  public List<OtcPropertyGroup> getGroups() {
    return groups;
  }

  public void addGroup(OtcPropertyGroup group) {
    this.groups.add(group);
  }

  public void addProperty(OtcProperty property) {
    this.otcProperties.add(property);
  }

  /**
   * Return all properties of group and it's children properties
   * @return list of properties
   */
  public List<OtcProperty> getAllOtcProperties() {
    List<OtcProperty> all = new ArrayList<>(otcProperties);
    groups.forEach(group -> all.addAll(group.getAllOtcProperties()));
    return all;
  }
}

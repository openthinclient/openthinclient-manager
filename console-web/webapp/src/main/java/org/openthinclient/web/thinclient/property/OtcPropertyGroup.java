package org.openthinclient.web.thinclient.property;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.openthinclient.common.model.schema.Node;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;

public class OtcPropertyGroup {

  private String label;
  private String tip;
  private List<OtcProperty> otcProperties = new ArrayList<>();
  private List<OtcPropertyGroup> groups   = new ArrayList<>();

  public OtcPropertyGroup(String label, String tip) {
    this.label = label;
    this.tip = tip;
  }

  public OtcPropertyGroup() {
    new OtcPropertyGroup(null, null);
  }

  public List<OtcProperty> getOtcProperties() {
    return otcProperties;
  }

  public String getLabel() {
    return label;
  }

  public String getTip() {
    return tip;
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

  public void removeProperty(String key) {
    otcProperties.stream().filter(otcProperty -> otcProperty.getKey().equals(key))
                          .findFirst()
                          .ifPresent(otcProperty -> otcProperties.remove(otcProperty));
  }

  public Optional<OtcProperty> getProperty(String key) {
    return otcProperties.stream().filter(otcProperty -> otcProperty.getKey().equals(key)).findFirst();
  }

  /**
   * Return this group and all child-groups recursive
   * @return
   */
  public List<OtcPropertyGroup> getAllOtcPropertyGroups() {
    List<OtcPropertyGroup> all = new ArrayList<>(groups);
    groups.forEach(group -> all.addAll(group.getAllOtcPropertyGroups()));
    return all;
  }

}

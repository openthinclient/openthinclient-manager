package org.openthinclient.web.thinclient.property;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;

/**
 *
 */
public class OtcPropertyGroup {

  private final String label;
  private List<OtcProperty> otcProperties = new ArrayList<>();
  private List<OtcPropertyGroup> groups   = new ArrayList<>();

  private Consumer<ItemGroupPanel> valueWrittenConsumer;

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

  /**
   * Return this group and all child-groups recursive
   * @return
   */
  public List<OtcPropertyGroup> getAllOtcPropertyGroups() {
    List<OtcPropertyGroup> all = new ArrayList<>(groups);
    groups.forEach(group -> all.addAll(group.getAllOtcPropertyGroups()));
    return all;
  }

  public void setValueWrittenHandlerToAll(Consumer<ItemGroupPanel> consumer) {
    this.onValueWritten(consumer);
    getAllOtcPropertyGroups().forEach(group -> group.onValueWritten(consumer));
  }

  public void onValueWritten(Consumer<ItemGroupPanel> consumer) {
    valueWrittenConsumer = consumer;
  }

  public Consumer<ItemGroupPanel> getValueWrittenConsumer() {
    return valueWrittenConsumer;
  }

  public void addGroup(int index, OtcPropertyGroup group) {
      this.groups.add(index, group);
  }
}
package org.openthinclient.web.thinclient.property;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.component.PropertyComponent;

/**
 *
 */
public class OtcPropertyGroup {

  private final String label;
  private List<OtcProperty> otcProperties = new ArrayList<>();
  private List<OtcPropertyGroup> groups   = new ArrayList<>();

  private Consumer<ItemGroupPanel> valueWrittenConsumer;
  private Consumer<ItemGroupPanel> valueChangedConsumer;

  private boolean displayHeaderLabel = true;
  private boolean collapseOnDisplay = false;

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

  public boolean isCollapseOnDisplay() {
    return collapseOnDisplay;
  }

  public void setCollapseOnDisplay(boolean collapseOnDisplay) {
    this.collapseOnDisplay = collapseOnDisplay;
  }

  /**
   * Dislay the clickable (expand/collapse) header-label (i.e. 'Settings') of a property-group, default is true
   * @return displayHeaderLabel
   */
  public boolean isDisplayHeaderLabel() {
    return displayHeaderLabel;
  }

  /**
   * Dislay the clickable (expand/collapse) header-label (i.e. 'Settings') of a property-group or not
   * @param displayHeaderLabel default is true
   */
  public void setDisplayHeaderLabel(boolean displayHeaderLabel) {
    this.displayHeaderLabel = displayHeaderLabel;
  }

//  public void onValueChanged(Consumer<ItemGroupPanel> consumer) {
//    valueChangedConsumer = consumer;
//  }
//
//  public Consumer<ItemGroupPanel> getValueChangedConsumer() {
//    return valueChangedConsumer;
//  }

}

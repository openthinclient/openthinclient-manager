package org.openthinclient.web.thinclient.component;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import org.openthinclient.web.thinclient.property.*;

import java.util.*;

/**
 * Group of properties
 */
public class ItemGroupPanel extends VerticalLayout {

  private Map<PropertyComponent, Component> propertyComponents = new HashMap<>();

  public ItemGroupPanel(OtcPropertyGroup propertyGroup) {
    setMargin(false);
    setSpacing(false);
    setStyleName("itemGroupPanel");

    if (propertyGroup.getLabel() != null) {
      AbstractLayout head = new CssLayout();
      head.addComponents(new Label(propertyGroup.getLabel()));
      if (propertyGroup.getTip() != null) {
        Button button = new Button(null, VaadinIcons.INFO_CIRCLE_O);
        button.addStyleNames("context-info-button", "borderless");
        Label info = new Label(propertyGroup.getTip(), ContentMode.HTML);
        info.setStyleName("context-info-label");
        head.addComponents(button, info);
      }
      head.setStyleName("itemGroupHeader");
      addComponent(head);
    }

    // compose properties
    propertyGroup.getOtcProperties().forEach(p -> addProperty(p, 0));
    // compose sub-group-properties
    propertyGroup.getGroups().forEach(pg -> addProperty(pg, 1));
  }

  public ItemGroupPanel(List<OtcProperty> otcProperties) {
    setMargin(false);
    setSpacing(false);
    setStyleName("itemGroupPanel");

    // compose only properties
    otcProperties.forEach(p -> addProperty(p, 0));
  }

  /**
   * Creates a property-edit line
   * @param property
   * @param level
   */
  public void addProperty(OtcProperty property, int level) {
    Layout proprow = new CssLayout();
    proprow.addStyleNames("property", "property-" + level);

    // label and property-component
    Label propertyLabel = new Label(property.getLabel());
    propertyLabel.setStyleName("propertyLabel");
    proprow.addComponent(propertyLabel);
    PropertyComponent pc = createPropertyComponent(property);
    proprow.addComponent(pc);

    // info
    if (property.getTip() != null) {
      Button showSettingInfoButton = new Button(null, VaadinIcons.INFO_CIRCLE_O);
      showSettingInfoButton.addStyleNames("context-info-button", "borderless");

      Label currentSettingInfo = new Label(property.getTip(), ContentMode.HTML);
      currentSettingInfo.setStyleName("context-info-label");
      proprow.addComponents(showSettingInfoButton, currentSettingInfo);
    }

    // and validation
    Label validationLabel = new Label(property.getLabel());
    validationLabel.setStyleName("validationLabel");
    proprow.addComponent(validationLabel);

    propertyComponents.put(pc, validationLabel);
    addComponent(proprow);
  }

  /**
   * Creates a line with Label for a property-group
   * @param propertyGroup
   * @param level
   */
  public void addProperty(OtcPropertyGroup propertyGroup, int level) {
    if (propertyGroup.getLabel() != null) {
      AbstractLayout row = new CssLayout();
      row.addComponents(new Label(propertyGroup.getLabel()));
      if (propertyGroup.getTip() != null) {
        Button button = new Button(null, VaadinIcons.INFO_CIRCLE_O);
        button.addStyleNames("context-info-button", "borderless");
        Label info = new Label(propertyGroup.getTip(), ContentMode.HTML);
        info.setStyleName("context-info-label");
        row.addComponents(button, info);
      }
      row.addStyleNames("propertyGroupLabel", "propertyGroupLabel-" + level);
      addComponent(row);
    }
    propertyGroup.getOtcProperties().forEach(p -> addProperty(p, level));
    propertyGroup.getGroups().forEach(pg -> addProperty(pg, level + 1));
  }

  /**
   * Create form-component from property
   * @param property  OtcProperty
   * @return PropertyComponent for given property-type
   */
  private PropertyComponent createPropertyComponent(OtcProperty property) {
    if (property instanceof OtcPasswordProperty) {
      return new PropertyPasswordField<>((OtcPasswordProperty) property);
    } else if (property instanceof OtcTextProperty) {
      return new PropertyTextField<>((OtcTextProperty) property);
    } else if (property instanceof OtcOptionProperty) {
      return new PropertySelect<>((OtcOptionProperty) property);
    } else if (property instanceof OtcMacProperty) {
      return new PropertyMacSelect<>((OtcMacProperty) property);
    }
    throw new RuntimeException("Unknown Property-Type: " + property);
  }

  public List<PropertyComponent> propertyComponents() {
    return new ArrayList<>(propertyComponents.keySet());
  }

  /**
   * Find first PropertyComponent of ItemGroupPanel
   * @param key the key of OtcProperty
   * @return Optional<PropertyComponent>
   */
  public Optional<PropertyComponent> getPropertyComponent(String key) {
    return propertyComponents.keySet().stream().filter(pc -> ((OtcProperty) pc.getBinder().getBean()).getKey().equals(key)).findFirst();
  }

  /**
   * Display validation message for key
   * @param key - the key of property
   * @param message - message to display
   */
  public void setValidationMessage(String key, String message) {
    getPropertyComponent(key).ifPresent(propertyComponent -> {
      Label component = (Label) propertyComponents.get(propertyComponent);
      component.setValue(message);
      component.setVisible(true);
    });
  }

  /**
   * Empty all validation messages
   */
  public void emptyValidationMessages() {
    propertyComponents.forEach( (propertyComponent, component) -> component.setVisible(false) );
  }

}

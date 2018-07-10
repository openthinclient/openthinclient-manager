package org.openthinclient.web.thinclient.component;

import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.ui.*;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Group of properties
 */
public class ItemGroupPanel extends VerticalLayout {

  private static final Logger LOGGER = LoggerFactory.getLogger(ItemGroupPanel.class);

  private Label infoLabel;
  private NativeButton save;
  private NativeButton reset;
  private NativeButton head;

  boolean itemsVisible = false;
  private List<PropertyComponent> propertyComponents = new ArrayList<>();

  public ItemGroupPanel(OtcPropertyGroup propertyGroup) {

    setMargin(false);

    setStyleName("itemGroupPanel");
    head = new NativeButton(propertyGroup.getLabel() !=  null ? propertyGroup.getLabel() : "Einstellungen");
    head.setStyleName("headButton");
    head.setSizeFull();
    addComponent(head);

    propertyGroup.getOtcProperties().forEach(p -> addProperty(p, 0));
    if (propertyGroup.getOtcProperties().isEmpty()) { // hässlich 2: nur weil die Schemas keine einheitliche Hirarchie haben
      propertyGroup.getGroups().forEach(pg -> addProperty(pg, 1));
    }

    buildActionsBar();

    collapseItems();
  }

  public void addProperty(OtcProperty property, int level) {
    HorizontalLayout proprow = new HorizontalLayout();
    proprow.setStyleName("property-" + level);
    Label propertyLabel = new Label(property.getLabel());
    propertyLabel.setStyleName("propertyLabel");
    proprow.addComponent(propertyLabel);
    PropertyComponent pc = createPropertyComponent(property);
    proprow.addComponent(pc);
    proprow.addComponent(new Label(property.getConfiguration().getKey() + "=" + property.getConfiguration().getValue()));
//    rows.addComponent(proprow, rows.getComponentCount() - 2);
    propertyComponents.add(pc);
    addComponent(proprow);
  }

  public void addProperty(OtcPropertyGroup propertyGroup, int level) {
    if (propertyGroup.getLabel() != null) {
      Label groupLabel = new Label(propertyGroup.getLabel());
      groupLabel.setStyleName("propertyGroupLabel-" + level);
//      rows.addComponent(groupLabel, rows.getComponentCount() - 2);
      addComponent(groupLabel);
    }
    propertyGroup.getOtcProperties().forEach(p -> addProperty(p, level));
    propertyGroup.getGroups().forEach(pg -> addProperty(pg, level + 1));
  }


  public void collapseItems() {
    itemsVisible = false;
    head.removeStyleName("itemsVisible");
    int componentCount = getComponentCount();
    for(int i=1; i<componentCount; i++) {
      getComponent(i).setVisible(false);
    }
  }

  public void expandItems() {
    itemsVisible = true;
    head.addStyleName("itemsVisible");
    int componentCount = getComponentCount();
    for(int i=1; i<componentCount; i++) {
      getComponent(i).setVisible(true);
    }
  }

  /**
   * Create form-components form property
   * @param property
   * @return
   */
  private PropertyComponent createPropertyComponent(OtcProperty property) {
    if (property instanceof OtcBooleanProperty) {
      return new PropertyCheckBox<>((OtcBooleanProperty) property);
    } else if (property instanceof OtcTextProperty) {
      return new PropertyTextField<>((OtcTextProperty) property);
    } else if (property instanceof OtcOptionProperty) {
      return new PropertySelect<>((OtcOptionProperty) property);
    }
    throw new RuntimeException("Unknown Property-Type: " + property);
  }

  private void buildActionsBar() {

    // Button bar
    save = new NativeButton("Save");
    save.addStyleName("profile_save");
    reset = new NativeButton("Reset");
    reset.addStyleName("profile_reset");
    HorizontalLayout actions = new HorizontalLayout();
    actions.addStyleName("actionBar");
    actions.addComponents(reset, save);
    addComponent(actions);

    addComponent(infoLabel = new Label());
    infoLabel.setStyleName("itemGroupInfoLabel");
    infoLabel.setVisible(false);

  }

  public void setError(String caption) {
    infoLabel.setVisible(true);
    infoLabel.setCaption(caption);
    infoLabel.removeStyleName("form_success");
    infoLabel.addStyleName("form_error");
  }

  public void setInfo(String caption) {
    infoLabel.setVisible(true);
    infoLabel.setCaption(caption);
    infoLabel.removeStyleName("form_error");
    infoLabel.addStyleName("form_success");
  }

  public List<PropertyComponent> propertyComponents() {
    return propertyComponents;
  }

  public boolean isItemsVisible() {
    return itemsVisible;
  }

  public NativeButton getSave() {
    return save;
  }

  public NativeButton getReset() {
    return reset;
  }

  public NativeButton getHead() {
    return head;
  }

  public Label getInfoLabel() {
    return infoLabel;
  }

  @Override
  public String toString() {
    return "ItemGrouPanel: '" + infoLabel.getValue() + "', Properties: " + propertyComponents();
  }
}

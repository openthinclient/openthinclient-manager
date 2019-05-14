package org.openthinclient.web.thinclient.component;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

/**
 * Group of properties
 */
public class ItemGroupPanel extends VerticalLayout implements CollapseablePanel {

  private static final Logger LOGGER = LoggerFactory.getLogger(ItemGroupPanel.class);

  private final IMessageConveyor mc;
  private NativeButton head;

  boolean itemsVisible = false;
  private Map<PropertyComponent, Component> propertyComponents = new HashMap<>();
  /** index to start collapsing/expanding items */
  private int itemStartIndex = 1;

  public ItemGroupPanel(OtcPropertyGroup propertyGroup) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    setMargin(false);
    setSpacing(false);
    setStyleName("itemGroupPanel");
    head = new NativeButton(propertyGroup.getLabel() !=  null ? propertyGroup.getLabel() : mc.getMessage(UI_THINCLIENT_SETTINGS));
    head.setStyleName("headButton");
    head.setSizeFull();

    if (propertyGroup.isDisplayHeaderLabel()) {
      addComponent(head);
    } else {
      addStyleName("headButtonHidden");
      itemStartIndex = 0;
    }

    // compose properties
    propertyGroup.getOtcProperties().forEach(p -> addProperty(p, 0));
    // compose sub-group-properties
    propertyGroup.getGroups().forEach(pg -> addProperty(pg, 1));

    // buildActionsBar();

    if (propertyGroup.isCollapseOnDisplay()) {
      collapseItems();
    }
  }

  /**
   * Creates a property-edit line
   * @param property
   * @param level
   */
  public void addProperty(OtcProperty property, int level) {
    HorizontalLayout proprow = new HorizontalLayout();
    proprow.setSpacing(false);
    proprow.setStyleName("property-" + level);

    // label and property-component
    Label propertyLabel = new Label(property.getLabel());
    propertyLabel.setStyleName("propertyLabel");
    proprow.addComponent(propertyLabel);
    PropertyComponent pc = createPropertyComponent(property);
    proprow.addComponent(pc);

    // info
    if (property.getTip() != null) {
      Button showSettingInfoButton = new Button(null, VaadinIcons.INFO_CIRCLE_O);
      showSettingInfoButton.setStyleName("borderless-colored");

      Label currentSettingInfo = new Label(property.getTip());
      currentSettingInfo.setVisible(false);
      currentSettingInfo.setStyleName("propertyInformationLabel");
      proprow.addComponents(showSettingInfoButton, currentSettingInfo);
      showSettingInfoButton.addClickListener(clickEvent -> currentSettingInfo.setVisible(!currentSettingInfo.isVisible()));
    }

    // and validation
    Label validationLabel = new Label(property.getLabel());
    validationLabel.setStyleName("validationLabel");
    validationLabel.setVisible(false);
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
      Label groupLabel = new Label(propertyGroup.getLabel());
      groupLabel.setStyleName("propertyGroupLabel-" + level);
      addComponent(groupLabel);
    }
    propertyGroup.getOtcProperties().forEach(p -> addProperty(p, level));
    propertyGroup.getGroups().forEach(pg -> addProperty(pg, level + 1));
  }


  public void collapseItems() {
    itemsVisible = false;
    head.removeStyleName("itemsVisible");
    int componentCount = getComponentCount();
    for(int i=itemStartIndex; i<componentCount; i++) {
      getComponent(i).setVisible(false);
    }
  }

  public void expandItems() {
    itemsVisible = true;
    head.addStyleName("itemsVisible");
    int componentCount = getComponentCount();
    for(int i=itemStartIndex; i<componentCount; i++) {
      getComponent(i).setVisible(true);
    }
  }

  /**
   * Create form-components form property and add valueChangeListener
   * @param property  OtcProperty
   * @return PropertyComponent for given property-type
   */
  private PropertyComponent createPropertyComponent(OtcProperty property) {
    if (property instanceof OtcBooleanProperty) {
      PropertyToggleSlider<OtcBooleanProperty> field = new PropertyToggleSlider<>((OtcBooleanProperty) property);
      // TODO enable svabe-button of paren
//      field.getBinder().addValueChangeListener(e -> save.setEnabled(true));
      return field;

    } else if (property instanceof OtcPasswordProperty) {
      PropertyPasswordField<OtcPasswordProperty> field = new PropertyPasswordField<>((OtcPasswordProperty) property);
      // TODO enable svabe-button of paren
//      field.getBinder().addValueChangeListener(e -> save.setEnabled(true));
      return field;

    } else if (property instanceof OtcTextProperty) {
      PropertyTextField<OtcTextProperty> field = new PropertyTextField<>((OtcTextProperty) property);
      // TODO enable svabe-button of paren
//      field.getBinder().addValueChangeListener(e -> save.setEnabled(true));
      return field;

    } else if (property instanceof OtcOptionProperty) {
      PropertySelect<OtcOptionProperty> field = new PropertySelect<>((OtcOptionProperty) property);
      // TODO enable svabe-button of parent
//      field.getBinder().addValueChangeListener(e -> save.setEnabled(true));
      return field;
    }
    throw new RuntimeException("Unknown Property-Type: " + property);
  }

  public List<PropertyComponent> propertyComponents() {
    return new ArrayList<>(propertyComponents.keySet());
  }

  public boolean isItemsVisible() {
    return itemsVisible;
  }

//  public NativeButton getSave() {
//    return save;
//  }
//
//  public NativeButton getReset() {
//    return reset;
//  }
//
//  public Label getInfoLabel() {
//    return infoLabel;
//  }

  public NativeButton getHead() {
    return head;
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
    propertyComponents.forEach((propertyComponent, component) -> {
      component.setVisible(false);
    });
  }

}

package org.openthinclient.web.thinclient;

import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.VerticalLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 */
public class OtcPropertyLayout {

  VerticalLayout rows;
  List<PropertyComponent> propertyComponents = new ArrayList();

  public OtcPropertyLayout() {

    rows = new VerticalLayout();
    buildActionsBar();

  }

  public void addProperty(OtcProperty property) {
    addProperty(property, 0);
  }

  public void addProperty(OtcProperty property, int level) {
    HorizontalLayout proprow = new HorizontalLayout();
    proprow.setStyleName("property-" + level);
    Label propertyLabel = new Label(property.getLabel());
    propertyLabel.setStyleName("propertyLabel");
    proprow.addComponent(propertyLabel);
    PropertyComponent pc = createPropertyComponent(property);
    proprow.addComponent(pc);
    rows.addComponent(proprow, rows.getComponentCount() - 1);
    propertyComponents.add(pc);
  }

  public void addProperty(OtcPropertyGroup otcPropertyGroup) {
    addProperty(otcPropertyGroup, 0);
  }

  public void addProperty(OtcPropertyGroup propertyGroup, int level) {
    Label groupLabel = new Label(propertyGroup.getLabel());
    groupLabel.setStyleName("propertyGroupLabel-" + level);
    rows.addComponent(groupLabel, rows.getComponentCount() - 1);
    propertyGroup.getOtcProperties().forEach(p -> addProperty(p, level));
    propertyGroup.getGroups().forEach(pg -> addProperty(pg, level + 1));
  }


  private PropertyComponent createPropertyComponent(OtcProperty property) {
    if (property instanceof OtcBooleanProperty) {
      return new BooleanPropertyPanel<>((OtcBooleanProperty) property);
    } else if (property instanceof OtcTextProperty) {
      return new TextPropertyPanel<>((OtcTextProperty) property);
    } else if (property instanceof OtcOptionProperty) {
      return new OptionPropertyPanel<>((OtcOptionProperty) property);
    }
    throw new RuntimeException("Unknown Property-Type: " + property);
  }

  public void addProperties(OtcProperty... props) {
    Arrays.asList(props).forEach(this::addProperty);
  }

  public void addComponent(Component component) {
    rows.addComponent(component, rows.getComponentCount() - 1);
  }

  private void buildActionsBar() {

    // Button bar
    NativeButton save = new NativeButton("Save");
    NativeButton reset = new NativeButton("Reset");
    HorizontalLayout actions = new HorizontalLayout();
    actions.addComponents(save, reset);
    rows.addComponent(actions);

    // Click listeners for the buttons
    save.addClickListener(event -> {
      propertyComponents.forEach(bc -> {
        if (bc.getBinder().writeBeanIfValid(bc.getBinder().getBean())) {
          Notification.show("Saved");
        } else {
          BinderValidationStatus<?> validate = bc.getBinder().validate();
          String errorText = validate.getFieldValidationStatuses()
              .stream().filter(BindingValidationStatus::isError)
              .map(BindingValidationStatus::getMessage)
              .map(Optional::get).distinct()
              .collect(Collectors.joining(", "));
          Notification.show("There are errors: " + errorText, Type.ERROR_MESSAGE);
        }
      });
    });
    reset.addClickListener(event -> {
      // clear fields by setting null
      propertyComponents.forEach(propertyComponent -> propertyComponent.getBinder().readBean(null));
    });
  }

  public Component getContent() {
    return rows;
  }


}

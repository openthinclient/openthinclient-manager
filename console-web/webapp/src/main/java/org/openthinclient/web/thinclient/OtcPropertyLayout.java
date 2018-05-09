package org.openthinclient.web.thinclient;

import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.server.Sizeable.Unit;
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
    HorizontalLayout vl = new HorizontalLayout();
    Label label = new Label(property.getLabel());
    label.setWidth(250, Unit.PIXELS);
    vl.addComponent(label);
    PropertyComponent pc = createPropertyComponent(property);
    vl.addComponent(pc);
    rows.addComponent(vl, rows.getComponentCount() - 1);
    propertyComponents.add(pc);
  }

  public void addProperty(OtcPropertyGroup otcPropertyGroup) {
    rows.addComponent(new Label(otcPropertyGroup.getLabel()), rows.getComponentCount() - 1);
    otcPropertyGroup.getOtcProperties().forEach(this::addProperty);
  }


  private PropertyComponent createPropertyComponent(OtcProperty property) {
    if (property instanceof OtcBooleanProperty) {
      return new BooleanPropertyPanel<>((OtcBooleanProperty) property);
    } else if (property instanceof OtcTextProperty) {
      return new TextPropertyPanel<>((OtcTextProperty) property);
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

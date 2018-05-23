package org.openthinclient.web.thinclient;

import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openthinclient.common.model.Profile;
import org.openthinclient.web.thinclient.component.PropertyCheckBox;
import org.openthinclient.web.thinclient.component.PropertyComponent;
import org.openthinclient.web.thinclient.component.PropertySelect;
import org.openthinclient.web.thinclient.component.PropertyTextField;
import org.openthinclient.web.thinclient.property.OtcBooleanProperty;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;

/**
 *
 */
public class ProfileFormLayout {

  Panel formPanel;
  VerticalLayout rows;
  List<PropertyComponent> propertyComponents = new ArrayList();

  public ProfileFormLayout(String name, Class<? extends Profile> clazz) {


    rows = new VerticalLayout();
//    rows.setMargin(false);

    formPanel = new Panel(name);
    formPanel.setContent(rows);
    formPanel.addStyleName("formPanel_" + clazz.getSimpleName());

//    if (name != null) {
//      Label label = new Label(name);
//      label.setStyleName(ValoTheme.LABEL_H2);
//      rows.addComponent(label);
//    }
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
    if (propertyGroup.getLabel() != null) {
      Label groupLabel = new Label(propertyGroup.getLabel());
      groupLabel.setStyleName("propertyGroupLabel-" + level);
      rows.addComponent(groupLabel, rows.getComponentCount() - 1);
    }
    propertyGroup.getOtcProperties().forEach(p -> addProperty(p, level));
    propertyGroup.getGroups().forEach(pg -> addProperty(pg, level + 1));
  }


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

  public void addComponent(Component component) {
    rows.addComponent(component, rows.getComponentCount() - 1);
  }

  private void buildActionsBar() {

    // Button bar
    NativeButton save = new NativeButton("Save");
    save.addStyleName("profile_save");
    NativeButton reset = new NativeButton("Reset");
    reset.addStyleName("profile_reset");
    HorizontalLayout actions = new HorizontalLayout();
    actions.addStyleName("actionBar");
    actions.addComponents(reset, save);
    rows.addComponent(actions);

    // Click listeners for the buttons
    save.addClickListener(event -> {
      final List<String> errors = new ArrayList<>();
      propertyComponents.forEach(bc -> {
        if (bc.getBinder().writeBeanIfValid(bc.getBinder().getBean())) {
//          Notification.show("Saved");
        } else {
          BinderValidationStatus<?> validate = bc.getBinder().validate();
          String errorText = validate.getFieldValidationStatuses()
              .stream().filter(BindingValidationStatus::isError)
              .map(BindingValidationStatus::getMessage)
              .map(Optional::get).distinct()
              .collect(Collectors.joining(", "));
//          Notification.show("There are errors: " + errorText, Type.ERROR_MESSAGE);
          errors.add(errorText);
        }
      });
      if (errors.isEmpty()) {
        onSuccess();
      } else {
        Notification.show("There are errors: " + errors, Type.ERROR_MESSAGE);
      }
    });
    reset.addClickListener(event -> {
      // clear fields by setting null
      propertyComponents.forEach(propertyComponent -> propertyComponent.getBinder().readBean(null));
    });
  }

  public void onSuccess() { }

  public Component getContent() {
    return formPanel;
  }


}

package org.openthinclient.web.thinclient;

import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openthinclient.web.thinclient.component.PropertyCheckBox;
import org.openthinclient.web.thinclient.component.PropertyComponent;
import org.openthinclient.web.thinclient.component.PropertySelect;
import org.openthinclient.web.thinclient.component.PropertyTextField;
import org.openthinclient.web.thinclient.property.OtcBooleanProperty;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ProfileFormLayout {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileFormLayout.class);

  Panel formPanel;
  VerticalLayout rows;
  List<PropertyComponent> propertyComponents = new ArrayList();
  Label infoLabel;
  private Runnable valuesWrittenCallback;
  private Runnable valuesSavedCallback;

  public ProfileFormLayout(String name, Class clazz) {


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
    proprow.addComponent(new Label(property.getConfiguration().getKey() + "=" + property.getConfiguration().getValue()));
    rows.addComponent(proprow, rows.getComponentCount() - 2);
    propertyComponents.add(pc);
  }

  public void addProperty(OtcPropertyGroup otcPropertyGroup) {
    addProperty(otcPropertyGroup, 0);
  }

  public void addProperty(OtcPropertyGroup propertyGroup, int level) {
    if (propertyGroup.getLabel() != null) {
      Label groupLabel = new Label(propertyGroup.getLabel());
      groupLabel.setStyleName("propertyGroupLabel-" + level);
      rows.addComponent(groupLabel, rows.getComponentCount() - 2);
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
    rows.addComponent(component, rows.getComponentCount() - 2);
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

    infoLabel = new Label();
    rows.addComponent(infoLabel);

    // Click listeners for the buttons
    save.addClickListener(event -> {
      final List<String> errors = new ArrayList<>();
      propertyComponents.forEach(bc -> {
        if (bc.getBinder().writeBeanIfValid(bc.getBinder().getBean())) {
          LOGGER.debug("Bean valid " + bc.getBinder().getBean());
        } else {
          BinderValidationStatus<?> validate = bc.getBinder().validate();
          String errorText = validate.getFieldValidationStatuses()
              .stream().filter(BindingValidationStatus::isError)
              .map(BindingValidationStatus::getMessage)
              .map(Optional::get).distinct()
              .collect(Collectors.joining(", "));
          errors.add(errorText + "\n");
        }
      });
      if (errors.isEmpty()) {
        valuesWrittenCallback.run();
      } else {
        StringBuilder sb = new StringBuilder();
        errors.forEach(sb::append);
        setError(sb.toString());
      }
    });

    // clear fields by setting null
    reset.addClickListener(event -> {
      propertyComponents.forEach(propertyComponent -> propertyComponent.getBinder().readBean(null));
    });
  }

  public void setError(String caption) {
    infoLabel.setCaption(caption);
    infoLabel.setStyleName("form_error");
  }

  public void setInfo(String caption) {
    infoLabel.setCaption(caption);
    infoLabel.setStyleName("form_success");
  }

  public void onBeanValuesWritten(Runnable callback) {
    this.valuesWrittenCallback = callback;
  }

  public void onValuesSaved(Runnable callback) {
    this.valuesSavedCallback = callback;
  }


  public Component getContent() {
    return formPanel;
  }


  public void valuesSaved() {
    valuesSavedCallback.run();
  }
}

package org.openthinclient.web.thinclient;

import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.ui.*;
import org.openthinclient.web.thinclient.component.PropertyCheckBox;
import org.openthinclient.web.thinclient.component.PropertyComponent;
import org.openthinclient.web.thinclient.component.PropertySelect;
import org.openthinclient.web.thinclient.component.PropertyTextField;
import org.openthinclient.web.thinclient.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 */
public class ProfilePanel extends  Panel {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePanel.class);

//  Panel formPanel;
  VerticalLayout rows;
  List<PropertyComponent> propertyComponents = new ArrayList();
  Label infoLabel;
  private Runnable valuesWrittenCallback;
  private Runnable valuesSavedCallback;

  public ProfilePanel(String name, Class clazz) {

    super(name);

    rows = new VerticalLayout();

    setContent(rows);
    addStyleName("formPanel_" + clazz.getSimpleName());



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


  public void valuesSaved() {
    valuesSavedCallback.run();
  }
}

package org.openthinclient.web.thinclient.component;

import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.ui.*;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.property.OtcBooleanProperty;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcTextProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ItemGroupPanel extends VerticalLayout {

  private static final Logger LOGGER = LoggerFactory.getLogger(ItemGroupPanel.class);


  List<PropertyComponent> propertyComponents = new ArrayList();
  Label infoLabel;
  private Runnable valuesWrittenCallback;

  NativeButton head;

  boolean itemsVisible = false;

  public ItemGroupPanel(ProfilePanel profilePanel) {

    setMargin(false);

    setStyleName("itemGroupPanel");
    head = new NativeButton("Allgemeinse");
    head.setStyleName("headButton");
    head.setSizeFull();
    head.addClickListener(clickEvent -> {
      if (itemsVisible) {
        collapseItemGroup();
      } else {
        expandItemGroup();
        profilePanel.handleItemGroupVisibility(this);
      }
    });

    addComponent(head);

    // samples
    addComponent(createPropertyComponent(new OtcTextProperty("Property 1", "p1", new ItemConfiguration("p1", "Test"))));
    addComponent(createPropertyComponent(new OtcTextProperty("Property 2", "p2", new ItemConfiguration("p2", "Test 2"))));
    addComponent(createPropertyComponent(new OtcTextProperty("Property 3", "p3", new ItemConfiguration("p3", "Test - 3"))));

    buildActionsBar();

    collapseItemGroup();
  }

  public void collapseItemGroup() {
    itemsVisible = false;
    head.removeStyleName("itemsVisible");
    int componentCount = getComponentCount();
    for(int i=1; i<componentCount; i++) {
      getComponent(i).setVisible(false);
    }
  }

  public void expandItemGroup() {
    itemsVisible = true;
    head.addStyleName("itemsVisible");
    int componentCount = getComponentCount();
    for(int i=1; i<componentCount; i++) {
      getComponent(i).setVisible(true);
    }
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

  private void buildActionsBar() {

    // Button bar
    NativeButton save = new NativeButton("Save");
    save.addStyleName("profile_save");
    NativeButton reset = new NativeButton("Reset");
    reset.addStyleName("profile_reset");
    HorizontalLayout actions = new HorizontalLayout();
    actions.addStyleName("actionBar");
    actions.addComponents(reset, save);
    addComponent(actions);

    addComponent(infoLabel = new Label());
    infoLabel.setEnabled(false);

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
        // TODO: show error
        // setError(sb.toString());
      }
    });

    // clear fields by setting null
    reset.addClickListener(event -> {
      propertyComponents.forEach(propertyComponent -> propertyComponent.getBinder().readBean(null));
    });
  }

  public boolean isItemsVisible() {
    return itemsVisible;
  }

}

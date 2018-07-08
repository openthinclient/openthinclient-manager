package org.openthinclient.web.thinclient;

import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import org.openthinclient.web.thinclient.component.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 */
public class ProfilePanel extends Panel {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePanel.class);

  VerticalLayout rows;
  List<PropertyComponent> propertyComponents = new ArrayList();
  Label infoLabel;
  private Runnable valuesWrittenCallback;
  private Runnable valuesSavedCallback;

  public ProfilePanel(String name, Class clazz) {

    super(name);

    setContent(rows = new VerticalLayout());
    rows.setMargin(false);
    rows.setSpacing(false);

    setWidth(95, Unit.PERCENTAGE);
    setStyleName("profilePanel");
    addStyleName("formPanel_" + clazz.getSimpleName());

    setItemGroups();
  }

  public void setItemGroups() {

    ItemGroupPanel itemGroupPanel = new ItemGroupPanel();

    rows.addComponent(itemGroupPanel);
    rows.addComponent(new ItemGroupPanel());

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

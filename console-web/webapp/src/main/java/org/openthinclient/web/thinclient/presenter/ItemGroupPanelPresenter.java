package org.openthinclient.web.thinclient.presenter;

import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.ui.Button;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.component.PropertyComponent;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemGroupPanelPresenter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ItemGroupPanelPresenter.class);

  ProfilePanel profilePanel;
  private ItemGroupPanel view;
  private Consumer<ItemGroupPanel> valuesWrittenConsumer;

  public ItemGroupPanelPresenter(ProfilePanel profilePanel, ItemGroupPanel view) {
    this.profilePanel = profilePanel;
    this.view = view;

    view.getSave().addClickListener(this::save);
    view.getReset().addClickListener(this::reset);
    view.getHead().addClickListener(this::handleItemVisibility);
  }

  // Click listeners for the buttons
  void save(Button.ClickEvent event) {

      view.emptyValidationMessages();
      view.getInfoLabel().setCaption("");

      final List<String> errors = new ArrayList<>();
      for (PropertyComponent bc : view.propertyComponents()) {

        if (bc.getBinder().writeBeanIfValid(bc.getBinder().getBean())) {
          LOGGER.debug("Bean valid " + bc.getBinder().getBean());
        } else {
          BinderValidationStatus<?> validate = bc.getBinder().validate();
          String errorText = validate.getFieldValidationStatuses()
                  .stream().filter(BindingValidationStatus::isError)
                  .map(BindingValidationStatus::getMessage)
                  .map(Optional::get)
                  .distinct()
                  .collect(Collectors.joining(", "));
          errors.add(errorText);

          OtcProperty bean = (OtcProperty) bc.getBinder().getBean();
          view.setValidationMessage(bean.getKey(), errorText);
        }
      }

      if (errors.isEmpty()) {
        valuesWrittenConsumer.accept(view);
      } else {
        view.setError("Not saved, please check validation errors.");
      }
  }

  // clear fields by setting null
  void reset(Button.ClickEvent event) {
    view.emptyValidationMessages();
    view.getInfoLabel().setCaption("");

    view.propertyComponents().forEach(propertyComponent -> {
      OtcProperty bean = (OtcProperty) propertyComponent.getBinder().getBean();
      bean.getConfiguration().setValue(bean.getInitialValue());
      propertyComponent.getBinder().readBean(bean);
    });
  }

  public void handleItemVisibility(Button.ClickEvent clickEvent) {
    if (view.isItemsVisible()) {
      view.collapseItems();
    } else {
      view.expandItems();
      profilePanel.handleItemGroupVisibility(view);
    }
  }

  public void setValuesWrittenConsumer(Consumer<ItemGroupPanel> consumer) {
    this.valuesWrittenConsumer = consumer;
  }
}
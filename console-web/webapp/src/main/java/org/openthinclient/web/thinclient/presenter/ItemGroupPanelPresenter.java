package org.openthinclient.web.thinclient.presenter;

import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.ui.Button;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
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

      view.getInfoLabel().setCaption("");
      view.getInfoLabel().setVisible(false);

      final List<String> errors = new ArrayList<>();
      view.propertyComponents().forEach(bc -> {
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
        valuesWrittenConsumer.accept(view);
      } else {
        StringBuilder sb = new StringBuilder();
        errors.forEach(sb::append);
        view.setError(sb.toString());
      }
  }

  // clear fields by setting null
  void reset(Button.ClickEvent event) {
    view.getInfoLabel().setCaption("");
    view.getInfoLabel().setVisible(false);
    view.propertyComponents().forEach(propertyComponent -> propertyComponent.getBinder().readBean(null));
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
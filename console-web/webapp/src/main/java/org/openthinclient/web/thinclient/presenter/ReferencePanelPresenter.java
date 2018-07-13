package org.openthinclient.web.thinclient.presenter;

import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.BindingValidationStatus;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.Button;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.component.ReferencePanel;
import org.openthinclient.web.thinclient.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ReferencePanelPresenter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencePanelPresenter.class);

  ProfilePanel profilePanel;
  private ReferencePanel view;
  private Consumer<ReferencePanel> profileReferenceChanged;
  ListDataProvider<Item> itemListDataProvider;

  public ReferencePanelPresenter(ProfilePanel profilePanel, ReferencePanel view, Profile profile, List<Item> items) {
    this.profilePanel = profilePanel;
    this.view = view;

    view.getHead().addClickListener(this::handleItemVisibility);
    view.getClientsComboBox().addValueChangeListener(this::itemSelected);
    itemListDataProvider = new ListDataProvider<>(items);
    view.getClientsComboBox().setDataProvider(itemListDataProvider);

  }

  private void itemSelected(HasValue.ValueChangeEvent<Item> event) {
    view.getReferenceLine().addComponent(new Button(event.getValue().getName()), view.getReferenceLine().getComponentCount() - 1);
    itemListDataProvider.getItems().remove(event.getValue());
  }

  public void handleItemVisibility(Button.ClickEvent clickEvent) {
    if (view.isItemsVisible()) {
      view.collapseItems();
    } else {
      view.expandItems();
      profilePanel.handleItemGroupVisibility(view);
    }
  }

  public void setProfileReferenceChangedConsumer(Consumer<ReferencePanel> consumer) {
    this.profileReferenceChanged = consumer;
  }
}
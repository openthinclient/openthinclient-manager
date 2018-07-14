package org.openthinclient.web.thinclient.presenter;

import com.vaadin.data.HasValue;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.Button;
import org.openthinclient.web.thinclient.ProfilePanel;
import org.openthinclient.web.thinclient.component.ReferencePanel;
import org.openthinclient.web.thinclient.component.ReferencesComponent;
import org.openthinclient.web.thinclient.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class ReferenceComponentPresenter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceComponentPresenter.class);

  private ReferencesComponent view;
  private Consumer<ReferencePanel> profileReferenceChanged;
  ListDataProvider<Item> itemListDataProvider;

  public ReferenceComponentPresenter(ReferencesComponent view, List<Item> allItems, List<Item> referencedItems) {
    this.view = view;

    this.view.getClientsComboBox().addValueChangeListener(this::itemSelected);

    allItems.removeAll(referencedItems);
    itemListDataProvider = new ListDataProvider<>(allItems);
    this.view.getClientsComboBox().setDataProvider(itemListDataProvider);

    // referenced items
    referencedItems.forEach(this::addItemToView);

  }

  private void addItemToView(Item item) {
    Button button = view.addItemComponent(item.getName());
    button.addClickListener(clickEvent -> {
      view.removeItemComponent(item.getName()); // remove item

      // add item to selection-list to make it available
      itemListDataProvider.getItems().add(item);
      view.getClientsComboBox().setDataProvider(itemListDataProvider);

      view.getClientsComboBox().setValue(null); // vaadin-bug: https://github.com/vaadin/framework/issues/9047

    });
  }

  private void itemSelected(HasValue.ValueChangeEvent<Item> event) {
    addItemToView(event.getValue());

    itemListDataProvider.getItems().remove(event.getValue());
    view.getClientsComboBox().setDataProvider(itemListDataProvider);

    view.getClientsComboBox().setValue(null); // vaadin-bug: https://github.com/vaadin/framework/issues/9047

  // TODO: einzeln hinzufügen oder löschen -- oder : kompletten Zustand übergeben
  //    profileReferenceChanged.accept(event.getValue());
  }

  public void setProfileReferenceChangedConsumer(Consumer<ReferencePanel> consumer) {
    this.profileReferenceChanged = consumer;
  }
}
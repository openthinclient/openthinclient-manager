package org.openthinclient.web.thinclient.presenter;

import com.vaadin.data.HasValue;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.Button;
import org.openthinclient.web.thinclient.component.ReferencesComponent;
import org.openthinclient.web.thinclient.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class ReferenceComponentPresenter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceComponentPresenter.class);

  private ReferencesComponent view;
  private Consumer<List<Item>> profileReferenceChanged;
  ListDataProvider<Item> itemListDataProvider;
  /** item-subset which are referenced by the profile */
  private List<Item> currentReferencedItems;

  public ReferenceComponentPresenter(ReferencesComponent view, List<Item> allItems, List<Item> referencedItems) {
    this.view = view;
    this.currentReferencedItems = referencedItems;

    this.view.getItemComboBox().addValueChangeListener(this::itemSelected);
    this.view.getItemComboBox().setVisible(!allItems.isEmpty()); // hide if no entries available

    allItems.removeAll(referencedItems);
    itemListDataProvider = new ListDataProvider<>(allItems);
    this.view.getItemComboBox().setDataProvider(itemListDataProvider);

    // display referenced items
    referencedItems.forEach(this::addItemToView);

  }

  private void addItemToView(Item item) {
    Button button = view.addItemComponent(item.getName());
    button.addClickListener(clickEvent -> {

      currentReferencedItems.remove(item);
      profileReferenceChanged.accept(currentReferencedItems); // save

      view.removeItemComponent(item.getName()); // remove item

      // add item to selection-list to make it available
      itemListDataProvider.getItems().add(item);
      view.getItemComboBox().setDataProvider(itemListDataProvider);

      view.getItemComboBox().setValue(null); // vaadin-bug: https://github.com/vaadin/framework/issues/9047

    });
  }

  private void itemSelected(HasValue.ValueChangeEvent<Item> event) {

    currentReferencedItems.add(event.getValue());
    profileReferenceChanged.accept(currentReferencedItems); // save

    addItemToView(event.getValue());

    itemListDataProvider.getItems().remove(event.getValue());
    view.getItemComboBox().setDataProvider(itemListDataProvider);

    view.getItemComboBox().setValue(null); // vaadin-bug: https://github.com/vaadin/framework/issues/9047

  }

  public void setProfileReferenceChangedConsumer(Consumer<List<Item>> consumer) {
    this.profileReferenceChanged = consumer;
  }
}
package org.openthinclient.web.thinclient.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.*;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.component.ItemButtonComponent;
import org.openthinclient.web.thinclient.component.ReferencesComponent;
import org.openthinclient.web.thinclient.model.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReferencesComponentPresenter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencesComponentPresenter.class);

  private IMessageConveyor mc;

  private ReferencesComponent view;
  private Consumer<List<Item>> profileReferenceChanged;
  ListDataProvider<Item> itemListDataProvider;
  /** item-subset which are referenced by the profile */
  private List<Item> currentReferencedItems;
  private Function<Item, List<Item>> memberSupplier;

  public ReferencesComponentPresenter(ReferencesComponent view, List<Item> allItems, List<Item> referencedItems) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    this.view = view;
    this.currentReferencedItems = referencedItems;

    this.view.getItemComboBox().addValueChangeListener(e -> this.itemSelected(e.getValue()));
    this.view.getItemComboBox().setVisible(!allItems.isEmpty()); // hide if no entries available

    this.view.getMultiSelectPopupBtn().addClickListener(this::handleMultiSelectPopup);

    allItems.removeAll(referencedItems);
    itemListDataProvider = new ListDataProvider<>(allItems);
    this.view.getItemComboBox().setDataProvider(itemListDataProvider);

    // display referenced items
    referencedItems.forEach(this::addItemToView);

  }

  private void addItemToView(Item item) {
    ItemButtonComponent button = view.addItemComponent(item.getName());
    button.addClickListener(clickEvent -> this.itemDeSelected(item));
  }

  private void itemSelected(Item item) {

    LOGGER.trace("Item selected: {}", item);

    currentReferencedItems.add(item);
    profileReferenceChanged.accept(currentReferencedItems); // save

    addItemToView(item);

    itemListDataProvider.getItems().remove(item);
    view.getItemComboBox().setDataProvider(itemListDataProvider);

//    view.getItemComboBox().setValue(null); // vaadin-bug: https://github.com/vaadin/framework/issues/9047
//    view.getItemComboBox().setSelectedItem(null);

    addMemberDetails(item);

  }

  private void itemDeSelected(Item item) {

    LOGGER.trace("Item de-selected: {}", item);

    currentReferencedItems.remove(item);
    profileReferenceChanged.accept(currentReferencedItems); // save

    view.removeItemComponent(item.getName()); // remove item

    // add item to selection-list to make it available
    itemListDataProvider.getItems().add(item);
    view.getItemComboBox().setDataProvider(itemListDataProvider);

//      view.getItemComboBox().setValue(null); // vaadin-bug: https://github.com/vaadin/framework/issues/9047
//      view.getItemComboBox().setSelectedItem(null);

    view.removeReferenceSublineComponent(item.getName());

  }

  private void handleMultiSelectPopup(Button.ClickEvent event) {

    Window multiSelectPopup = new Window();
    multiSelectPopup.setModal(true);
    multiSelectPopup.setResizable(false);
    multiSelectPopup.setClosable(false);

    VerticalLayout main = new VerticalLayout();
    main.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_COMMON_PLEASE_SELECT)));

    HorizontalLayout hl = new HorizontalLayout();

    List<Item> items = new ArrayList<>(itemListDataProvider.getItems());
    items.addAll(currentReferencedItems);

    ListDataProvider<Item> itemListDataProvider = new ListDataProvider<>(items);
    itemListDataProvider.setSortComparator(Comparator.comparing(Item::getName, String::compareToIgnoreCase)::compare);

    Grid<Item> referencesGrid = new Grid<>();
    referencesGrid.setDataProvider(itemListDataProvider);
    referencesGrid.setSelectionMode(Grid.SelectionMode.NONE);
    referencesGrid.removeHeaderRow(0);
    referencesGrid.addComponentColumn(this::createItemCheckBox);
    referencesGrid.setBodyRowHeight(40); // make sure the buttons fit in the cells of the Grid
    main.addComponent(referencesGrid);

    TextField filter = new TextField();
    filter.setPlaceholder(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_SEARCHFIELD_INPUTPROMT));
    filter.addValueChangeListener(event1 -> {
      itemListDataProvider.setFilter(Item::getName, s -> s.toLowerCase().contains(event1.getValue().toLowerCase()));
    });

    Button closeButton = new Button(mc.getMessage(ConsoleWebMessages.UI_BUTTON_CLOSE));
    closeButton.addClickListener(e -> multiSelectPopup.close());

    HorizontalLayout bottomLine = new HorizontalLayout();
    bottomLine.addComponents(filter, closeButton);
    main.addComponent(bottomLine);

    multiSelectPopup.setContent(main);

    UI.getCurrent().addWindow(multiSelectPopup);
  }

  private CheckBox createItemCheckBox(Item item) {
    CheckBox checkBox = new CheckBox(item.getName(), currentReferencedItems.contains(item));
    checkBox.addValueChangeListener(e -> {
      Boolean value = e.getValue();
      if (value) {
        this.itemSelected(item);
      } else {
        this.itemDeSelected(item);
      }
    });
    return checkBox;
  }

  public void setProfileReferenceChangedConsumer(Consumer<List<Item>> consumer) {
    this.profileReferenceChanged = consumer;
  }

  public void showSublineContent(Function<Item, List<Item>> memberSupplier) {
   this.memberSupplier = memberSupplier;
   currentReferencedItems.forEach(this::addMemberDetails);
   // hide multiselect-popup if readonly ist enabled
   this.view.getMultiSelectPopupBtn().setVisible(memberSupplier != null);
  }

  protected void addMemberDetails(Item item) {
    if (memberSupplier != null) {
      List<Item> members = memberSupplier.apply(item);
      List<ItemButtonComponent> components = members.stream().map(member -> new ItemButtonComponent(member.getName())).collect(Collectors.toList());
      this.view.addReferenceSublineComponents(item.getName(), components.toArray(new ItemButtonComponent[]{}));
    }
  }
}
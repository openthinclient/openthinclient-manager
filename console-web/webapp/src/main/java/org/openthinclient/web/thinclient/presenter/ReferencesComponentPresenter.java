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
  private boolean isReadOnly;

  public ReferencesComponentPresenter(ReferencesComponent view, List<Item> allItems, List<Item> referencedItems, Function<Item, List<Item>> memberSupplier, boolean isReadOnly) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    this.view = view;
    this.currentReferencedItems = referencedItems;
    this.memberSupplier = memberSupplier;
    this.isReadOnly = isReadOnly;

    this.view.getMultiSelectPopupBtn().addClickListener(this::handleMultiSelectPopup);
    // hide multiselect-popup if readonly ist enabled
    this.view.getMultiSelectPopupBtn().setVisible(!isReadOnly);

    itemListDataProvider = new ListDataProvider<>(allItems);

    // display referenced items
    for (Item item : referencedItems) {
      addItemToView(item);
      addMemberDetails(item);
    }

  }

  private void addItemToView(Item item) {
    ItemButtonComponent button = view.addItemComponent(item.getName(), isReadOnly);
    button.addClickListener(clickEvent -> this.itemDeSelected(item));
  }

  private void itemSelected(Item item) {

    LOGGER.debug("Item selected: {}", item);

    currentReferencedItems.add(item);
    profileReferenceChanged.accept(currentReferencedItems); // save

    addItemToView(item);
    addMemberDetails(item);

  }

  private void itemDeSelected(Item item) {

    LOGGER.debug("Item de-selected: {}", item);

    currentReferencedItems.remove(item);
    profileReferenceChanged.accept(currentReferencedItems); // save

    view.removeItemComponent(item.getName()); // remove item
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

  protected void addMemberDetails(Item item) {
    if (memberSupplier != null) {
      List<Item> members = memberSupplier.apply(item);
      List<ItemButtonComponent> components = members.stream().map(member -> new ItemButtonComponent(member.getName(), true)).collect(Collectors.toList());
      this.view.addReferenceSublineComponents(item.getName(), components.toArray(new ItemButtonComponent[]{}));
    }
  }
}
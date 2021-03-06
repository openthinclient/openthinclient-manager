package org.openthinclient.web.thinclient.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.ui.*;
import com.vaadin.server.SerializablePredicate;

import org.openthinclient.web.component.Popup;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.component.ItemButtonComponent;
import org.openthinclient.web.thinclient.component.ReferencesComponent;
import org.openthinclient.web.thinclient.model.Item;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReferencesComponentPresenter {

  private IMessageConveyor mc;

  private ReferencesComponent view;
  private Consumer<List<Item>> profileReferenceChanged;
  ListDataProvider<Item> itemListDataProvider;
  /** item-subset which are referenced by the profile */
  private List<Item> currentReferencedItems;
  private Function<Item, List<Item>> memberSupplier;

  private CheckBox selectAll;
  private AllCheckBoxes allCheckBoxes;
  private boolean checkBoxesIgnoreValueChange = false;
  private boolean selectAllIgnoreValueChange = false;
  private Query<Item, SerializablePredicate<Item>> queryUnreferenced;
  private Query<Item, SerializablePredicate<Item>> queryReferenced;
  private Query<Item, SerializablePredicate<Item>> queryAll;

  public ReferencesComponentPresenter(ReferencesComponent view, List<Item> allItems, List<Item> referencedItems, Function<Item, List<Item>> memberSupplier, boolean isReadOnly) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    this.view = view;
    this.currentReferencedItems = referencedItems;
    this.memberSupplier = memberSupplier;

    if(!isReadOnly) {
      this.view.getMultiSelectPopupBtn().addClickListener(this::handleMultiSelectPopup);
    }

    itemListDataProvider = new ListDataProvider<>(allItems);
    itemListDataProvider.setSortComparator(Comparator.comparing(Item::getName, String::compareToIgnoreCase)::compare);

    allCheckBoxes = new AllCheckBoxes();
    queryUnreferenced = new Query<>(item -> !currentReferencedItems.contains(item));
    queryReferenced = new Query<>(item -> currentReferencedItems.contains(item));
    queryAll = new Query<>();

    // display referenced items
    for (Item item : referencedItems) {
      addItemToView(item);
      addMemberDetails(item);
    }

  }

  private void addItemToView(Item item) {
    ItemButtonComponent button = view.addItemComponent(item);
    button.addClickListener(clickEvent -> this.itemsDeSelected(Arrays.asList(item)));
  }

  private void itemsSelected(List<Item> items) {
    currentReferencedItems.addAll(items);
    profileReferenceChanged.accept(currentReferencedItems); // save

    items.forEach(item -> {
      addItemToView(item);
      addMemberDetails(item);
    });
  }

  private void itemsDeSelected(List<Item> items) {
    currentReferencedItems.removeAll(items);
    profileReferenceChanged.accept(currentReferencedItems); // save

    items.forEach(item -> {
      view.removeItemComponent(item.getName()); // remove item
      view.removeReferenceSublineComponent(item.getName());
    });
  }

  private void handleMultiSelectPopup(Button.ClickEvent event) {

    Popup multiSelectPopup = new Popup(ConsoleWebMessages.UI_COMMON_PLEASE_SELECT, "reference-popup");

    CssLayout filterLine = new CssLayout();
    filterLine.setStyleName("filterLine");
    TextField filter = new TextField();
    filter.setPlaceholder(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_SEARCHFIELD_INPUTPROMPT));
    filter.addValueChangeListener(event1 -> {
      itemListDataProvider.setFilter(Item::getName, s -> s.toLowerCase().contains(event1.getValue().toLowerCase()));
      updateSelectAll();
    });
    itemListDataProvider.clearFilters();
    filterLine.addComponent(filter);
    multiSelectPopup.addContent(filterLine);

    selectAll = new CheckBox(mc.getMessage(ConsoleWebMessages.UI_COMMON_SELECT_ALL), false);
    selectAll.setStyleName("selectAll");
    selectAll.addValueChangeListener(e -> {
      if (!selectAllIgnoreValueChange) {
        checkBoxesIgnoreValueChange = true;
        if (e.getValue()) {
          itemListDataProvider.fetch(queryUnreferenced).forEach(item -> {
            allCheckBoxes.setValue(item, true);
          });
          itemsSelected(itemListDataProvider.fetch(queryUnreferenced).collect(Collectors.toList()));
        } else {
          itemListDataProvider.fetch(queryReferenced).forEach(item -> {
            allCheckBoxes.setValue(item, false);
          });
          itemsDeSelected(itemListDataProvider.fetch(queryReferenced).collect(Collectors.toList()));
        }
        checkBoxesIgnoreValueChange = false;
      }
    });
    updateSelectAll();
    multiSelectPopup.addContent(selectAll);

    CssLayout table = new CssLayout();
    table.setStyleName("table");
    Grid<Item> referencesGrid = new Grid<>();
    referencesGrid.setDataProvider(itemListDataProvider);
    referencesGrid.setSelectionMode(Grid.SelectionMode.NONE);
    referencesGrid.removeHeaderRow(0);
    referencesGrid.addComponentColumn(this::createItemCheckBox);
    referencesGrid.setBodyRowHeight(32);
    table.addComponent(referencesGrid);
    multiSelectPopup.addContent(table);

    multiSelectPopup.open();
    filter.focus();
  }

  private void updateSelectAll() {
    selectAllIgnoreValueChange = true;
    if (itemListDataProvider.fetch(queryAll).findAny().isPresent()) {
      selectAll.setValue(
        itemListDataProvider.fetch(queryAll).allMatch(currentReferencedItems::contains));
    } else {
      selectAll.setValue(false);
    }
    selectAllIgnoreValueChange = false;
  }

  private CheckBox createItemCheckBox(Item item) {
    CheckBox checkBox = new CheckBox(item.getName(), currentReferencedItems.contains(item));
    allCheckBoxes.add(item, checkBox);
    checkBox.addValueChangeListener(e -> {
      if(!checkBoxesIgnoreValueChange) {
        Boolean value = e.getValue();
        if (value) {
          this.itemsSelected(Arrays.asList(item));
        } else {
          this.itemsDeSelected(Arrays.asList(item));
        }
        updateSelectAll();
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
      List<ItemButtonComponent> components = members.stream().map(member -> new ItemButtonComponent(member)).collect(Collectors.toList());
      this.view.addReferenceSublineComponents(item.getName(),
                                              components.toArray(new ItemButtonComponent[]{}));
    }
  }

  private class AllCheckBoxes {
    private Map<Item, WeakReference<CheckBox>> map = new HashMap();
    private Optional<CheckBox> get(Item item) {
      return Optional.ofNullable(map.get(item)).map(WeakReference::get);
    }
    void add(Item item, CheckBox checkBox) {
      map.put(item, new WeakReference(checkBox));
    }
    void setValue(Item item, boolean value) {
      get(item).ifPresent(checkBox -> checkBox.setValue(value));
    }
    boolean getValue(Item item) {
      return get(item).map(CheckBox::getValue).orElse(false);
    }
  }
}

package org.openthinclient.web.thinclient.component;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.MultiSelectionModel;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.AbstractThinclientView;
import org.openthinclient.web.thinclient.ProfilePropertiesBuilder;
import org.openthinclient.web.thinclient.exception.AllItemsListException;
import org.vaadin.viritin.button.MButton;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_COMMON_DELETE;

public class ProfilesListOverviewPanel extends Panel {


  private IMessageConveyor mc;
  private CheckBox selectAll;
  private Grid<DirectoryObject> itemGrid;

  private Button addNew;
  private Button deleteProfileAction;

  private Consumer<DirectoryObject> itemButtonClickedConsumer = null;
  private Grid.Column<DirectoryObject, Button> itemBtn;

  public ProfilesListOverviewPanel(ConsoleWebMessages i18nTitleKey) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    setCaption(mc.getMessage(i18nTitleKey));
    addStyleName("overviewPanel");
    setVisible(false);

    VerticalLayout vl = new VerticalLayout();
    setContent(vl);

    TextField filter = new TextField();
    filter.addStyleNames("profileItemFilter");
    filter.setPlaceholder(mc.getMessage(UI_PACKAGEMANAGER_SEARCHFIELD_INPUTPROMT));
    filter.setIcon(VaadinIcons.FILTER);
    filter.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
    filter.addValueChangeListener(this::onFilterTextChange);
    vl.addComponent(filter);

    HorizontalLayout actionLine = new HorizontalLayout();
    selectAll = new CheckBox(mc.getMessage(UI_COMMON_SELECT_ALL));
    selectAll.addValueChangeListener(this::selectAllItems);
    actionLine.addComponent(selectAll);

    addNew = new Button("");
    addNew.setIcon(VaadinIcons.PLUS_CIRCLE_O);
    addNew.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    addNew.addStyleName(ValoTheme.BUTTON_SMALL);
    addNew.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
//    addNew.addClickListener(e -> UI.getCurrent().getNavigator().navigateTo(viewName + "/create"));
    actionLine.addComponent(addNew);

    deleteProfileAction = new Button();
    deleteProfileAction.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_ALT_TEXT_DELETE));
    deleteProfileAction.setIcon(VaadinIcons.MINUS_CIRCLE_O);
    deleteProfileAction.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    deleteProfileAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
//    deleteProfileAction.addClickListener(this::handleDeleteAction);
    actionLine.addComponent(deleteProfileAction);
    vl.addComponent(actionLine);

    itemGrid = new Grid<>();
    itemGrid.setSelectionMode(Grid.SelectionMode.MULTI);
    itemGrid.addColumn(DirectoryObject::getName);
    itemBtn = itemGrid.addComponentColumn(dirObj -> {
      Button button = new Button();
      button.setIcon(VaadinIcons.TOUCH);
      button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
      button.addStyleName(ValoTheme.BUTTON_SMALL);
      button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      button.addClickListener(e -> itemButtonClicked(dirObj));
      return button;
    });
    itemGrid.removeHeaderRow(0);
    itemGrid.setSizeFull();
    itemGrid.setHeightMode(com.vaadin.shared.ui.grid.HeightMode.UNDEFINED);
    // Profile-Type based style
    itemGrid.setStyleGenerator(profile -> profile.getClass().getSimpleName());

    vl.addComponent(itemGrid);
  }

  private void itemButtonClicked(DirectoryObject dirObj) {
    if (itemButtonClickedConsumer != null) {
      itemButtonClickedConsumer.accept(dirObj);
    }
  }

//  private Label createErrorLabel(Exception e) {
//    Label emptyScreenHint = new Label(
//        VaadinIcons.WARNING.getHtml() + "&nbsp;&nbsp;&nbsp;" + mc.getMessage(ConsoleWebMessages.UI_THINCLIENTS_HINT_ERROR) + e.getMessage(),
//        ContentMode.HTML);
//    emptyScreenHint.setStyleName("errorScreenHint");
//    return emptyScreenHint;
//  }



  private void selectAllItems(HasValue.ValueChangeEvent<Boolean> booleanValueChangeEvent) {
    MultiSelectionModel<DirectoryObject> selectionModel = (MultiSelectionModel<DirectoryObject>) itemGrid.getSelectionModel();
    if (booleanValueChangeEvent.getValue()) {
      selectionModel.selectAll();
    } else {
      selectionModel.deselectAll();
    }
  }

  private void onFilterTextChange(HasValue.ValueChangeEvent<String> event) {
    ListDataProvider<DirectoryObject> dataProvider = (ListDataProvider<DirectoryObject>) itemGrid.getDataProvider();
    long groupHeader = dataProvider.getItems().stream().filter(i -> i.getClass().equals(ProfilePropertiesBuilder.MenuGroupProfile.class)).count();
    dataProvider.setFilter(directoryObject -> {
      if (directoryObject instanceof ProfilePropertiesBuilder.MenuGroupProfile) {
        return true;
      } else {
        return caseInsensitiveContains(directoryObject.getName(), event.getValue());
      }
    });
  }

  private Boolean caseInsensitiveContains(String where, String what) {
    return where.toLowerCase().contains(what.toLowerCase());
  }


  public Grid<DirectoryObject> getItemGrid() {
    return itemGrid;
  }

  public Button getAddButton() {
    return addNew;
  }

  public Button getDeleteButton() {
    return deleteProfileAction;
  }

  public CheckBox getCheckBox() {
    return selectAll;
  }

  public void setItemButtonClickedConsumer(Consumer<DirectoryObject> itemButtonClickedConsumer) {
    this.itemButtonClickedConsumer = itemButtonClickedConsumer;
    if (itemButtonClickedConsumer == null && itemBtn != null) { // hide Button-Column if no action is provided
      itemGrid.removeColumn(itemBtn);
    }
  }
}

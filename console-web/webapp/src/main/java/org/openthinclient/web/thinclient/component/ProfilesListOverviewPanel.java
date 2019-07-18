package org.openthinclient.web.thinclient.component;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.MultiSelectionModel;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.ProfilePropertiesBuilder;

import java.util.function.Consumer;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

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

    CssLayout layout = new CssLayout();
    layout.setSizeFull();
    setContent(layout);

    CssLayout filterLine = new CssLayout();
    filterLine.addStyleNames("filterLine");
    TextField filter = new TextField();
    filter.setPlaceholder(mc.getMessage(UI_PACKAGEMANAGER_SEARCHFIELD_INPUTPROMT));
    filter.addValueChangeListener(this::onFilterTextChange);
    filterLine.addComponent(filter);
    layout.addComponent(filterLine);

    HorizontalLayout actionLine = new HorizontalLayout();
    actionLine.addStyleNames("actionLine");
    selectAll = new CheckBox(mc.getMessage(UI_COMMON_SELECT_ALL));
    selectAll.addValueChangeListener(this::selectAllItems);
    actionLine.addComponent(selectAll);

    addNew = new Button("");
    addNew.setIcon(VaadinIcons.PLUS_CIRCLE_O);
    addNew.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    addNew.addStyleName(ValoTheme.BUTTON_SMALL);
    addNew.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    addNew.addStyleName("addNew");
//    addNew.addClickListener(e -> UI.getCurrent().getNavigator().navigateTo(viewName + "/create"));
    actionLine.addComponent(addNew);

    deleteProfileAction = new Button();
    deleteProfileAction.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_ALT_TEXT_DELETE));
    deleteProfileAction.setIcon(VaadinIcons.CLOSE_CIRCLE_O);
    deleteProfileAction.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    deleteProfileAction.addStyleName(ValoTheme.BUTTON_SMALL);
    deleteProfileAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    deleteProfileAction.addStyleName("deleteProfile");
//    deleteProfileAction.addClickListener(this::handleDeleteAction);
    actionLine.addComponent(deleteProfileAction);
    layout.addComponent(actionLine);

    CssLayout gridWrapper = new CssLayout();
    gridWrapper.addStyleNames("table");
    itemGrid = new Grid<>();
    // MULTI-Selection has Vaadin performance issue:
    // https://github.com/vaadin/vaadin-grid-flow/blob/master/vaadin-grid-flow/src/main/java/com/vaadin/flow/component/grid/GridMultiSelectionModel.java#L112
    itemGrid.setSelectionMode(Grid.SelectionMode.MULTI);
    itemGrid.addColumn(DirectoryObject::getName);
    itemBtn = itemGrid.addComponentColumn(dirObj -> {
      Button button = new Button(dirObj.getName());
      button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
      button.addClickListener(e -> itemButtonClicked(dirObj));
      return button;
    });
    itemGrid.addColumn(DirectoryObject::getDescription);
    itemGrid.removeHeaderRow(0);
    itemGrid.setSizeFull();
    itemGrid.setHeightMode(com.vaadin.shared.ui.grid.HeightMode.UNDEFINED);
    // Profile-Type based style
    itemGrid.setStyleGenerator(profile -> profile.getClass().getSimpleName());
    gridWrapper.addComponent(itemGrid);
    layout.addComponent(gridWrapper);

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

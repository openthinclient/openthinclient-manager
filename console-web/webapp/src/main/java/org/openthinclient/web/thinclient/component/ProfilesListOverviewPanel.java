package org.openthinclient.web.thinclient.component;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.MultiSelectionModel;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.ui.Table;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.ProfilePropertiesBuilder;
import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

public class ProfilesListOverviewPanel extends Panel {

  private IMessageConveyor mc;
  private CheckBox selectAll;
//  private Grid<DirectoryObject> itemGrid;
//  private ListSelect<DirectoryObject> multi;
  private CssLayout gridWrapper;
  private List<SelectionRow> selectionRows = new ArrayList<>();
  private ListDataProvider<DirectoryObject> dataProvider;

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
    actionLine.addComponent(deleteProfileAction);
    layout.addComponent(actionLine);

    gridWrapper = new CssLayout();
    gridWrapper.addStyleNames("table");

//    multi = new ListSelect<>();
//    multi.setItemCaptionGenerator(d -> "<a href=\"" + d.getName() + "\">" + d.getName() + "</a>");
//    multi.addValueChangeListener(event -> {
//      Notification.show("Number of selected items: " + event.getValue().size());
//    });
////    multi.setHtmlContentAllowed(true);
//    gridWrapper.addComponent(multi);
    layout.addComponent(gridWrapper);

    // MULTI-Selection has Vaadin performance issue:
    // https://github.com/vaadin/vaadin-grid-flow/issues/451 -
    // Änderungen wird scheinbar nur in Flow geben: https://github.com/vaadin/vaadin-grid-flow/pull/715
//    itemGrid = new Grid<>();
//    itemGrid.setSelectionMode(Grid.SelectionMode.MULTI);
//    itemGrid.addColumn(DirectoryObject::getName);
//    itemBtn = itemGrid.addComponentColumn(dirObj -> {
//      Button button = new Button(dirObj.getName());
//      button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
//      button.addClickListener(e -> itemButtonClicked(dirObj));
//      return button;
//    });
//    itemGrid.addColumn(DirectoryObject::getDescription);
//    itemGrid.removeHeaderRow(0);
//    itemGrid.setSizeFull();
//    itemGrid.setHeightMode(com.vaadin.shared.ui.grid.HeightMode.UNDEFINED);
//    // Profile-Type based style
//    itemGrid.setStyleGenerator(profile -> profile.getClass().getSimpleName());
//    gridWrapper.addComponent(itemGrid);
//    layout.addComponent(gridWrapper);

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
    if (booleanValueChangeEvent.getValue()) {
      selectionRows.forEach(SelectionRow::select);
    } else {
      selectionRows.forEach(SelectionRow::deselect);
    }
  }

  private void onFilterTextChange(HasValue.ValueChangeEvent<String> event) {
    selectAll.setValue(false);
    long groupHeader = dataProvider.getItems().stream().filter(i -> i.getClass().equals(ProfilePropertiesBuilder.MenuGroupProfile.class)).count();
    dataProvider.setFilter(directoryObject -> {
      if (directoryObject instanceof ProfilePropertiesBuilder.MenuGroupProfile) {
        return true;
      } else {
        final String what = event.getValue().toLowerCase();
        return Stream.of(directoryObject.getName(), directoryObject.getDescription())
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .anyMatch(where -> where.contains(what));
      }
    });

    updateRowList();

  }

  /**
   * Clears rows and add current data-provider-items to list
   */
  private void updateRowList() {
    // TODO improve filtering performance
    gridWrapper.removeAllComponents();
    selectionRows.clear();
    dataProvider.fetch(new Query<>()).collect(Collectors.toList()).forEach(directoryObject -> {
      SelectionRow selectionRow = new SelectionRow(directoryObject);
      gridWrapper.addComponent(selectionRow);
      selectionRows.add(selectionRow);
    });
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

  @Deprecated
  public void setItemButtonClickedConsumer(Consumer<DirectoryObject> itemButtonClickedConsumer) {
    this.itemButtonClickedConsumer = itemButtonClickedConsumer;
    if (itemButtonClickedConsumer == null && itemBtn != null) { // hide Button-Column if no action is provided
//      itemGrid.removeColumn(itemBtn);
      // TODO: remove item
    }
  }

  public void setDataProvider(ListDataProvider<DirectoryObject> dataProvider) {
    this.dataProvider = dataProvider;
    this.dataProvider.setSortComparator(Comparator.comparing(DirectoryObject::getName, String::compareToIgnoreCase)::compare);
    updateRowList();
  }

  /**
   * Return selected items
   * @return
   */
  public Set<DirectoryObject> getSelectedItems() {
    return selectionRows.stream()
        .filter(SelectionRow::isSelected)
        .map(SelectionRow::getDirectoryObject)
        .collect(Collectors.toSet());
  }

  class SelectionRow extends CustomComponent {
    private CheckBox cb;
    private DirectoryObject directoryObject;
    public SelectionRow(final DirectoryObject directoryObject) {
      this.directoryObject = directoryObject;
      CssLayout row = new CssLayout();
      row.addStyleName("columns");
      cb = new CheckBox();
      Button button = new Button();

      button.setCaptionAsHtml(true);
      StringBuilder caption = new StringBuilder("<span class=\"name\">")
          .append(HtmlUtils.htmlEscape(directoryObject.getName()))
          .append("</span>");
      String description = directoryObject.getDescription();
      if (description != null) {
        caption.append("\n\n<span class=\"description\">")
                .append(HtmlUtils.htmlEscape(description))
                .append("</span>");
      }
      button.setCaption(caption.toString());

      button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
      button.addClickListener(e -> itemButtonClicked(directoryObject));
      row.addComponents(cb, button);
      // The composition root MUST be set
      setCompositionRoot(row);
    }

    public void select() {
      cb.setValue(true);
    }

    public void deselect() {
      cb.setValue(false);
    }

    public boolean isSelected() { return cb.getValue(); }

    public DirectoryObject getDirectoryObject() {
      return directoryObject;
    }
  }
}

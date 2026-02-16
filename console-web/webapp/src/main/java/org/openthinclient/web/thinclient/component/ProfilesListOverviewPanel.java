package org.openthinclient.web.thinclient.component;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.common.model.ClientMetaData;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.ProfilePropertiesBuilder;
import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

public class ProfilesListOverviewPanel extends CssLayout {

  private IMessageConveyor mc;
  private CheckBox selectAll;
  private CssLayout gridWrapper;
  private List<SelectionRow> selectionRows = new ArrayList<>();
  private ListDataProvider<DirectoryObject> dataProvider;
  private Collection<String> onlineMACs = Collections.emptyList();

  private Button addNew;
  private Button deleteProfileAction;
  private Button ldifExportAction;
  private Button wolAction;
  private Button restartAction;
  private Button shutdownAction;
  private Button reapplySettingsAction;
  private boolean enabled = true;

  private Consumer<DirectoryObject> itemButtonClickedConsumer = null;
  private Grid.Column<DirectoryObject, Button> itemBtn;

  public ProfilesListOverviewPanel(ConsoleWebMessages i18nTitleKey, boolean enabled) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    this.enabled = enabled;

    setVisible(false);
    addStyleName("overviewPanel");

    CssLayout caption = new CssLayout();
    caption.addStyleName("caption");
    addComponent(caption);

    addNew = new Button(mc.getMessage(ConsoleWebMessages.UI_PROFILE_PANEL_NEW));
    addNew.addStyleNames(
      ValoTheme.BUTTON_SMALL,
      "create-new"
    );
    addNew.setVisible(enabled);

    caption.addComponents(
      new Label(mc.getMessage(i18nTitleKey)),
      addNew
    );

    CssLayout content = new CssLayout();
    content.addStyleName("content");
    addComponent(content);

    CssLayout filterLine = new CssLayout();
    filterLine.addStyleNames("filterLine");
    TextField filter = new TextField();
    filter.setPlaceholder(mc.getMessage(UI_PACKAGEMANAGER_SEARCHFIELD_INPUTPROMPT));
    filter.addValueChangeListener(this::onFilterTextChange);
    filterLine.addComponent(filter);
    content.addComponent(filterLine);

    CssLayout actionLine = new CssLayout();
    actionLine.addStyleNames("actionLine");
    actionLine.setVisible(enabled);
    content.addComponent(actionLine);

    selectAll = new CheckBox(mc.getMessage(UI_COMMON_SELECT_ALL));
    selectAll.addValueChangeListener(this::selectAllItems);
    actionLine.addComponent(selectAll);

    wolAction = new Button("");
    wolAction.setDescription("Wake On LAN");
    wolAction.setIcon(VaadinIcons.PLAY);
    wolAction.addStyleName(ValoTheme.BUTTON_BORDERLESS);
    wolAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    wolAction.addStyleName("wol");
    wolAction.setVisible(false);
    actionLine.addComponent(wolAction);

    restartAction = new Button("");
    restartAction.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_RESTART));
    restartAction.setIcon(VaadinIcons.REFRESH);
    restartAction.addStyleName(ValoTheme.BUTTON_BORDERLESS);
    restartAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    restartAction.addStyleName("restart");
    restartAction.setVisible(false);
    actionLine.addComponent(restartAction);

    shutdownAction = new Button("");
    shutdownAction.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_SHUTDOWN));
    shutdownAction.setIcon(VaadinIcons.STOP);
    shutdownAction.addStyleName(ValoTheme.BUTTON_BORDERLESS);
    shutdownAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    shutdownAction.addStyleName("shutdown");
    shutdownAction.setVisible(false);
    actionLine.addComponent(shutdownAction);

    Label spacer = new Label("");
    spacer.addStyleName("spacer");
    actionLine.addComponent(spacer);

    reapplySettingsAction = new Button("");
    reapplySettingsAction.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_REAPPLY_SETTINGS));
    reapplySettingsAction.setIcon(VaadinIcons.FLASH);
    reapplySettingsAction.addStyleName(ValoTheme.BUTTON_BORDERLESS);
    reapplySettingsAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    reapplySettingsAction.addStyleName("reapplySettings");
    reapplySettingsAction.setVisible(false);
    actionLine.addComponent(reapplySettingsAction);

    ldifExportAction = new Button("");
    ldifExportAction.setDescription("Export LDIF");
    ldifExportAction.setIcon(VaadinIcons.DOWNLOAD);
    ldifExportAction.addStyleName(ValoTheme.BUTTON_BORDERLESS);
    ldifExportAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    ldifExportAction.addStyleName("ldifExport");
    actionLine.addComponent(ldifExportAction);

    deleteProfileAction = new Button();
    deleteProfileAction.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_ALT_TEXT_DELETE));
    deleteProfileAction.setIcon(VaadinIcons.TRASH);
    deleteProfileAction.addStyleName(ValoTheme.BUTTON_BORDERLESS);
    deleteProfileAction.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    deleteProfileAction.addStyleName("deleteProfile");
    actionLine.addComponent(deleteProfileAction);

    gridWrapper = new CssLayout();
    gridWrapper.addStyleNames("table", "detectOverflow");
    content.addComponent(gridWrapper);
  }

  private void itemButtonClicked(DirectoryObject dirObj) {
    if (itemButtonClickedConsumer != null) {
      itemButtonClickedConsumer.accept(dirObj);
    }
  }

  private void selectAllItems(HasValue.ValueChangeEvent<Boolean> booleanValueChangeEvent) {
    if (booleanValueChangeEvent.getValue()) {
      selectionRows.forEach(SelectionRow::select);
    } else {
      selectionRows.forEach(SelectionRow::deselect);
    }
  }

  private void onFilterTextChange(HasValue.ValueChangeEvent<String> event) {
    selectAll.setValue(false);
    dataProvider.setFilter(directoryObject -> {
      if (directoryObject instanceof ProfilePropertiesBuilder.MenuGroupProfile) {
        return true;
      } else {
        final String what = event.getValue().toLowerCase();

        String[] data;
        if (directoryObject instanceof ClientMetaData) {
          ClientMetaData client = (ClientMetaData)directoryObject;
          String macAddress = client.getMacAddress();
          data = new String[]{
            client.getName(),
            client.getDescription(),
            macAddress, macAddress.replace(":", ""),
            client.getIpHostNumber()
          };
        } else {
          data = new String[]{directoryObject.getName(), directoryObject.getDescription()};
        }

        return Stream.of(data)
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

  public Button getLdifExportButton() {
    return ldifExportAction;
  }

  public Button getWolButton() {
    return wolAction;
  }

  public Button getRestartButton() {
    return restartAction;
  }

  public Button getShutdownButton() {
    return shutdownAction;
  }

  public Button getReapplySettingsButton() {
    return reapplySettingsAction;
  }

  public CheckBox getCheckBox() {
    return selectAll;
  }

  public void setItemButtonClickedConsumer(Consumer<DirectoryObject> itemButtonClickedConsumer) {
    this.itemButtonClickedConsumer = itemButtonClickedConsumer;
    if (itemButtonClickedConsumer == null && itemBtn != null) { // hide Button-Column if no action is provided
//      itemGrid.removeColumn(itemBtn);
      // TODO: remove item
    }
  }

  public void setDataProvider(ListDataProvider<DirectoryObject> dataProvider) {
    this.setDataProvider(dataProvider, Collections.emptyList());
  }

  public void setDataProvider(ListDataProvider<DirectoryObject> dataProvider, Collection<String> onlineMACs) {
    this.dataProvider = dataProvider;
    this.onlineMACs = onlineMACs;
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
      boolean isClient = directoryObject instanceof ClientMetaData;

      CssLayout row = new CssLayout();
      row.addStyleName("columns");

      cb = new CheckBox();
      cb.setVisible(enabled);

      Button button = new Button();
      button.setCaptionAsHtml(true);
      if (isClient) {
        String mac = ((ClientMetaData) directoryObject).getMacAddress();
        button.addStyleName("client");
        if (ClientService.DEFAULT_CLIENT_MAC.equals(mac)) {
          button.addStyleName("default");
        } else {
          if(onlineMACs.contains(mac)) {
            button.addStyleName("online");
          }
        }
      }

      StringBuilder caption = new StringBuilder();
      caption.append("<span class=\"name\">")
              .append(HtmlUtils.htmlEscape(directoryObject.getName()))
              .append("</span>");
      String description = directoryObject.getDescription();
      if (description != null && !description.isEmpty()) {
        caption.append("\n\n<span class=\"description\">")
        .append(HtmlUtils.htmlEscape(description))
        .append("</span>");
      }
      if (isClient) {
        caption.append("\n\n<span class=\"mac\">")
                .append(((ClientMetaData)directoryObject).getMacAddress())
                .append("</span>");
      }
      button.setCaption(caption.toString());

      button.addStyleNames(
        ValoTheme.BUTTON_BORDERLESS_COLORED,
        "profile-button"
      );
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

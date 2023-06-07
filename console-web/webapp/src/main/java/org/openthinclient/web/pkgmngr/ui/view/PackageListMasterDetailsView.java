package org.openthinclient.web.pkgmngr.ui.view;

import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.event.selection.MultiSelectionEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.components.grid.HeaderRow;
import com.vaadin.ui.renderers.ComponentRenderer;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.pkgmngr.ui.design.PackageListMasterDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGEMANAGER_SEARCHFIELD_INPUTPROMPT;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGEMANAGER_SHOW_ALL_VERSIONS;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGEMANAGER_SHOW_PREVIEW_VERSIONS;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public class PackageListMasterDetailsView extends PackageListMasterDetailsDesign implements PackageListMasterDetailsPresenter.View {

  private static final long serialVersionUID = 6572660094735789367L;

  private DataProvider<ResolvedPackageItem, ?> packageListDataProvider;
  private Consumer<Package> showPackageDetailsConsumer;
  private boolean detailsVisible;
  private float previousSplitPosition;
  private Unit previousSplitPositionUnit;
  private TextField filterInput;
  private CheckBox allPackagesFilterCheckbox;
  private CheckBox previewPackagesFilterCheckbox;

  public PackageListMasterDetailsView() {

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    setDataProvider(DataProvider.ofCollection(Collections.emptyList()));
    packageList.setSelectionMode(Grid.SelectionMode.MULTI);
    packageList.addColumn(AbstractPackageItem::getName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME)).setId("name");
    Column<ResolvedPackageItem, ?> versionColumn = packageList.addColumn(AbstractPackageItem::getDisplayVersion)
                                                              .setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION))
                                                              .setId("displayVersion");

    Column<ResolvedPackageItem, ?> detailsColumn = packageList.addColumn((ValueProvider<ResolvedPackageItem, Component>) item -> {
      final Button moreButton = new Button();
      moreButton.addStyleName("details-button");
      moreButton.setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_LIST_DETAILS_CAPTION));
      moreButton.setIcon(VaadinIcons.INFO_CIRCLE_O);
      moreButton.addStyleNames(ValoTheme.BUTTON_LINK);
      moreButton.addClickListener(e -> {
        if (showPackageDetailsConsumer != null && item instanceof ResolvedPackageItem)
          showPackageDetailsConsumer.accept(((ResolvedPackageItem)item).getPackage());
      });
      return moreButton;
    }, new ComponentRenderer())
    .setCaption("");

    packageList.addItemClickListener(event -> {
      // De/Select row on click on any column
      ResolvedPackageItem item = event.getItem();
      if (packageList.getSelectedItems().contains(item)) {
        packageList.deselect(item);
      } else {
        packageList.select(item);
      }
    });

    filterInput = new TextField();
    filterInput.setStyleName("filter-input");
    filterInput.setPlaceholder(mc.getMessage(UI_PACKAGEMANAGER_SEARCHFIELD_INPUTPROMPT));

    allPackagesFilterCheckbox = new CheckBox(mc.getMessage(UI_PACKAGEMANAGER_SHOW_ALL_VERSIONS));
    previewPackagesFilterCheckbox = new CheckBox(mc.getMessage(UI_PACKAGEMANAGER_SHOW_PREVIEW_VERSIONS));
    CssLayout toggles = new CssLayout();
    toggles.setStyleName("toggles");
    toggles.addComponents(
      allPackagesFilterCheckbox,
      previewPackagesFilterCheckbox
    );

    HeaderRow filterRow = packageList.addHeaderRowAt(0);
    filterRow.setStyleName("filter-row");
    filterRow.getCell("name").setComponent(filterInput);
    filterRow.join(
      Stream.of(versionColumn, detailsColumn).map(filterRow::getCell).collect(Collectors.toSet())
    ).setComponent(toggles);

    // prepare the initial state of the details view. It will be visible at the beginning.
    detailsVisible = true;
    // hide the details component
    setDetailsVisible(false);
    // specifying a default split a 70%
    previousSplitPosition = 70;
    previousSplitPositionUnit = Unit.PERCENTAGE;

    sourceUpdateButton.setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_SOURCE_UPDATE_NOW_BUTTON));
  }

  @Override
  public void setDataProvider(DataProvider<ResolvedPackageItem, ?> dataProvider) {
    packageListDataProvider = dataProvider;
    packageList.setDataProvider(packageListDataProvider);
  }

  @Override
  public void onPackageSelected(Consumer<Collection<Package>> consumer) {
    packageList.addSelectionListener(event -> {
      Set<ResolvedPackageItem> selectedItems = event.getAllSelectedItems();
      if (event instanceof MultiSelectionEvent) {  // this should always be true
        if (deduplicatePackageSelection(
            (MultiSelectionEvent<ResolvedPackageItem>)event)
        ) {
          // If the selection has been changed, abort and don't call the
          // consumer (yet), as the changes will be reflected in a new event.
          return;
        }
      }
      consumer.accept(
        selectedItems.stream()
            .map(item -> item.getPackage())
            .collect(Collectors.toCollection(ArrayList::new)));
    });
  }

  private boolean deduplicatePackageSelection(
      MultiSelectionEvent<ResolvedPackageItem> event
  ) {
    // Ensure that only one version of a package is selected.
    // Deselect all but one version of a package if multiple versions are
    // selected. Prefer user choice. If all fails prefer newer version.
    Set<ResolvedPackageItem> newlySelectedItems = event.getAddedSelection();
    if (newlySelectedItems.size() == 0) {
      return false;
    }

    Map<String, ResolvedPackageItem> selectCandidates = new HashMap<>();
    Set<ResolvedPackageItem> deselectItems = new HashSet<>();

    // first deduplicate the newly selected items
    for (ResolvedPackageItem item : newlySelectedItems) {
      if (!selectCandidates.containsKey(item.getName())) {
        selectCandidates.put(item.getName(), item);
      } else {
        // prefer newer version
        ResolvedPackageItem candidate = selectCandidates.get(item.getName());
        if (0 > candidate.getPackage().getVersion().compareTo(
            item.getPackage().getVersion())) {
          deselectItems.add(candidate);
          selectCandidates.put(item.getName(), item);
        } else {
          deselectItems.add(item);
        }
      }
    }
    // then deselect all packages that have newly selected other versions
    for (ResolvedPackageItem item : event.getAllSelectedItems()) {
      ResolvedPackageItem selectedItem = selectCandidates.get(item.getName());
      if (selectedItem != null && selectedItem != item) {
        deselectItems.add(item);
      }
    }

    if (deselectItems.size() > 0) {
      deselectItems.forEach(packageList::deselect);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void onShowPackageDetails(Consumer<Package> consumer) {
    showPackageDetailsConsumer = consumer;
  }

  public ComponentContainer getDetailsContainer() {
    return detailsContainer;
  }

  public TextField getFilterInput() {
    return filterInput;
  }

  @Override
  public CheckBox getAllPackagesFilterCheckbox() {
    return allPackagesFilterCheckbox;
  }

  @Override
  public CheckBox getPreviewPackagesFilterCheckbox() {
    return previewPackagesFilterCheckbox;
  }

  @Override
  public void sort(SortableProperty property, SortDirection direction) {
    packageList.sort(property.getBeanPropertyName(), direction);
  }

  @Override
  public void hideSourceUpdatePanel() {
    sourceUpdatePanel.setVisible(false);
  }

  @Override
  public void setSourceUpdateLabelValue(String text) {
    sourceUpdateLabel.setValue(text);
  }

  @Override
  public Button getSourceUpdateButton() {
    return sourceUpdateButton;
  }

  @Override
  public void clearSelection() {
    packageList.getSelectionModel().deselectAll();
  }

  @Override
  public void setDetailsVisible(boolean visible) {

    // no change at all.
    if (detailsVisible == visible)
      return;

    if(detailsVisible) {
      // the details view has been previously visible. Hide the view.
      previousSplitPosition = splitPanel.getSplitPosition();
      previousSplitPositionUnit = splitPanel.getSplitPositionUnit();
      // TODO when set to 100%, no appropriate repaint will occur. Are there better ways to achive this effect?
      splitPanel.setSplitPosition(99, Unit.PERCENTAGE);
    } else {
      // restore the previous split position
      splitPanel.setSplitPosition(previousSplitPosition, previousSplitPositionUnit);
    }
    detailsVisible = visible;
  }
}

package org.openthinclient.web.pkgmngr.ui.view;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.StyleGenerator;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;

import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.pkgmngr.ui.design.PackageDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsPresenter;

import java.util.Collections;
import java.util.List;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public class PackageDetailsView extends PackageDetailsDesign implements PackageDetailsPresenter.View {

  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = -2726203031530856857L;
  private final TabSheet.Tab tabDependencies;
  private final TabSheet.Tab tabProvides;
  private final TabSheet.Tab tabConflicts;
  private final TabSheet.Tab tabRelations;

  public PackageDetailsView() {

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    dependencies.setDataProvider(DataProvider.ofCollection(Collections.emptyList()));
    dependencies.setSelectionMode(Grid.SelectionMode.NONE);
    dependencies.addColumn(AbstractPackageItem::getName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    dependencies.addColumn(AbstractPackageItem::getDisplayVersion).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
    dependencies.setHeight("39px");

    dependencies.setStyleGenerator(new StyleGenerator<AbstractPackageItem>() {
      @Override
      public String apply(AbstractPackageItem item) {
        if (item != null && item instanceof MissingPackageItem) {
          return "highlight-red";
        }
        return null;
      }
    });

    // conflicts
    conflicts.setDataProvider(DataProvider.ofCollection(Collections.emptyList()));
    conflicts.setSelectionMode(Grid.SelectionMode.NONE);
    conflicts.addColumn(AbstractPackageItem::getName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    conflicts.addColumn(AbstractPackageItem::getDisplayVersion).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
    conflicts.setHeight("39px");

    // provides
    provides.setDataProvider(DataProvider.ofCollection(Collections.emptyList()));
    provides.setSelectionMode(Grid.SelectionMode.NONE);
    provides.addColumn(AbstractPackageItem::getName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    provides.addColumn(AbstractPackageItem::getDisplayVersion).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
    provides.setHeight("39px");

    this.changeLog.setContentMode(ContentMode.PREFORMATTED);

    // unfortunately this is the only way to access tabs defined in a design file.
    // we have to use the component instance to access the tab.

    // first the main tab sheet
    mainTabSheet.getTab(tabComponentCommon).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_DETAILS_COMMON_CAPTION));
    tabRelations = mainTabSheet.getTab(tabComponentRelations);
    tabRelations.setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_DETAILS_RELATIONS_CAPTION));
    mainTabSheet.getTab(tabComponentChangelog).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_DETAILS_CHANGELOG_CAPTION));

    // second the relations tab sheet.
    tabDependencies = relationsTabSheet.getTab(dependencies);
    tabDependencies.setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_DETAILS_RELATIONS_DEPENDENCIES_CAPTION));
    tabProvides = relationsTabSheet.getTab(provides);
    tabProvides.setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_DETAILS_RELATIONS_PROVIDES_CAPTION));
    tabConflicts = relationsTabSheet.getTab(conflicts);
    tabConflicts.setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_DETAILS_RELATIONS_CONFLICTS_CAPTION));

    updateRelationsTabs();
  }

  @Override
  public ComponentContainer getActionBar() {
    return inlineActionBar;
  }

  @Override
  public void setName(String name) {
    this.name.setValue(name);
  }

  @Override
  public void setVersion(String version) {
    this.version.setValue(version);
  }

  @Override
  public void setDescription(String description) {
    this.description.setValue(description);
  }

  @Override
  public void hide() {
    setVisible(false);
  }

  @Override
  public void show() {
    setVisible(true);
  }

  @Override
  public void setShortDescription(String shortDescription) {
    this.shortDescription.setValue(shortDescription);
  }

  @Override
  public void setSourceUrl(String url) {
    this.sourceUrl.setValue(url);
  }

  @Override
  public void setChangeLog(String changeLog) {
    this.changeLog.setValue(changeLog);
  }

  @Override
  public void hideConflictsTable() {
    conflicts.setVisible(false);
  }

  @Override
  public void hideProvidesTable() {
    provides.setVisible(false);
  }

  @Override
  public void addDependencies(List<AbstractPackageItem> depends) {
    if (depends != null) {
      dependencies.setDataProvider(DataProvider.ofCollection(depends));
      dependencies.setHeight(39 + (depends.size() * 38) + "px");
    }

    updateRelationsTabs();
  }

  private void updateRelationsTabs() {

    // 1) check if there are any relations. If not, hide the relations tab completely
    if (!hasDependencies() && !hasConflicts() && !hasProvides()) {
      tabRelations.setVisible(false);
      return;
    }
    tabRelations.setVisible(true);

    // check individual tabs
    tabProvides.setVisible(hasProvides());
    tabConflicts.setVisible(hasConflicts());
    tabDependencies.setVisible(hasDependencies());

  }

  private boolean hasProvides() {
    return provides.getDataProvider().size(new Query<>()) != 0;
  }

  private boolean hasConflicts() {
    return conflicts.getDataProvider().size(new Query<>()) != 0;
  }

  private boolean hasDependencies() {
    return dependencies.getDataProvider().size(new Query<>()) != 0;
  }

  @Override
  public void addConflicts(List<AbstractPackageItem> packageConflicts) {
    if (conflicts != null) {
      conflicts.setDataProvider(DataProvider.ofCollection(packageConflicts));
      conflicts.setHeight(39 + (packageConflicts.size() * 38) + "px");
    }
    updateRelationsTabs();
  }

  @Override
  public void addProvides(List<AbstractPackageItem> packageProvides) {
    if (provides != null) {
      provides.setDataProvider(DataProvider.ofCollection(packageProvides));
      provides.setHeight(39 + (packageProvides.size() * 38) + "px");
    }
    updateRelationsTabs();
  }
}

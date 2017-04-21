package org.openthinclient.web.pkgmngr.ui;

import com.vaadin.v7.data.Container;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Label;
import com.vaadin.v7.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.InstallPlanStep;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.PackageConflict;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.UnresolvedDependency;
import org.openthinclient.util.dpkg.PackageReference;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Stream.concat;

public class InstallationPlanSummaryDialog extends AbstractSummaryDialog {
  public static final String PROPERTY_TYPE = "type";
  public static final String PROPERTY_PACKAGE_NAME = "packageName";
  public static final String PROPERTY_PACKAGE_VERSION = "packageVersion";
  public static final String PROPERTY_INSTALLED_VERSION = "newVersion";
  private static final Logger LOG = LoggerFactory.getLogger(InstallationPlanSummaryDialog.class);
  private static final String PROPERTY_ICON = "icon";

  private final List<Runnable> onInstallListeners;

  private final Map<TableTypes, Table> tables;
  private final PackageManagerOperation packageManagerOperation;
  private final PackageManager packageManager;

  public InstallationPlanSummaryDialog(PackageManagerOperation packageManagerOperation, PackageManager packageManager) {
    super();

    this.packageManager = packageManager;
    this.packageManagerOperation = packageManagerOperation;
    tables = new HashMap<>();

    proceedButton.setCaption(getActionButtonCaption());
    // prevent install/uninstall if there are unresolved dependencies
    proceedButton.setEnabled(packageManagerOperation.getUnresolved().isEmpty());

    onInstallListeners = new ArrayList<>(2);
  }

  @Override
  protected void onCancel() {
    close();
  }

  private String getActionButtonCaption() {
    String actionButtonCaption = mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_INSTALL_BUTTON_CAPTION);
    if (packageManagerOperation.hasPackagesToUninstall()) {
      actionButtonCaption = mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_UNINSTALL_BUTTON_CAPTION);
    }
    return actionButtonCaption;
  }

  private String getHeadlineText() {
    String headlineText = mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_INSTALL_HEADLINE);
    if (packageManagerOperation.hasPackagesToUninstall()) {
      headlineText = mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_UNINSTALL_HEADLINE);
    }
    return headlineText;
  }

  @Override
  protected void createContent(MVerticalLayout content) {
    final Label l = new Label(getHeadlineText());
    l.addStyleName(ValoTheme.LABEL_HUGE);
    l.addStyleName(ValoTheme.LABEL_COLORED);
    content.addComponent(l);

    // install/uninstall
    tables.put(TableTypes.INSTALL_UNINSTALL, createTable());
    content.addComponent(new Label(getActionButtonCaption() + mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_ITEMS)));
    content.addComponent(tables.get(TableTypes.INSTALL_UNINSTALL));

    // conflicts
    if (!packageManagerOperation.getConflicts().isEmpty()) {
      tables.put(TableTypes.CONFLICTS, createTable());
      content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_CONFLICTS)));
      content.addComponent(tables.get(TableTypes.CONFLICTS));
    }

    // unresolved dependency
    if (!packageManagerOperation.getUnresolved().isEmpty()) {
      tables.put(TableTypes.UNRESOVED, createTable());
      if (packageManagerOperation.hasPackagesToUninstall()) {
        content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_DEPENDING_PACKAGE)));
      } else {
        content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_UNRESOLVED)));
      }
      content.addComponent(tables.get(TableTypes.UNRESOVED));
    }

    // suggested
    if (!packageManagerOperation.getSuggested().isEmpty()) {
      tables.put(TableTypes.SUGGESTED, createTable());
      content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_SUGGESTED)));
      content.addComponent(tables.get(TableTypes.SUGGESTED));
    }
  }

  /**
   * Creates a table with datasource of IndexedContainer
   *
   * @return the table
   */
  private Table createTable() {

    IndexedContainer packageItemContainer = new IndexedContainer();
    packageItemContainer.addContainerProperty(PROPERTY_ICON, Resource.class, null);
    packageItemContainer.addContainerProperty(PROPERTY_TYPE, Class.class, null);
    packageItemContainer.addContainerProperty(PROPERTY_PACKAGE_NAME, String.class, null);
    packageItemContainer.addContainerProperty(PROPERTY_PACKAGE_VERSION, String.class, null);
    packageItemContainer.addContainerProperty(PROPERTY_INSTALLED_VERSION, String.class, "");

    Table table = new Table();
    table.addStyleName(ValoTheme.TABLE_BORDERLESS);
    table.addStyleName(ValoTheme.TABLE_NO_HEADER);
    table.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
    table.addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
    table.setContainerDataSource(packageItemContainer);
    table.setItemIconPropertyId(PROPERTY_ICON);
    table.setVisibleColumns(PROPERTY_PACKAGE_NAME, PROPERTY_PACKAGE_VERSION
            // installed version is disabled for the moment, as the layout seems to be broken
//                , PROPERTY_INSTALLED_VERSION
    );
    table.setRowHeaderMode(Table.RowHeaderMode.ICON_ONLY);
    table.setColumnExpandRatio(PROPERTY_PACKAGE_NAME, 1);
//        table.setColumnExpandRatio(PROPERTY_PACKAGE_VERSION, 0.1f);
//        table.setColumnExpandRatio(PROPERTY_INSTALLED_VERSION, 0.1f);
//        table.setColumnWidth(PROPERTY_PACKAGE_VERSION, 100);
//        table.setColumnWidth(PROPERTY_INSTALLED_VERSION, 100);

    table.setWidth(100, Sizeable.Unit.PERCENTAGE);
    table.setHeight(100, Sizeable.Unit.PIXELS);

    return table;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void update() {

    // install/uninstall steps
    Table installTable = tables.get(TableTypes.INSTALL_UNINSTALL);
    Container installTableDataSource = installTable.getContainerDataSource();
    installTableDataSource.removeAllItems();
    for (InstallPlanStep step : packageManagerOperation.getInstallPlan().getSteps()) {
      final Item item = installTableDataSource.getItem(installTableDataSource.addItem());
      final Property<FontAwesome> iconProperty = item.getItemProperty(PROPERTY_ICON);
      final Property<String> installedVersionProperty = item.getItemProperty(PROPERTY_INSTALLED_VERSION);
      final Property<String> packageNameProperty = item.getItemProperty(PROPERTY_PACKAGE_NAME);
      final Property<String> packageVersionProperty = item.getItemProperty(PROPERTY_PACKAGE_VERSION);

      final Package pkg;
      if (step instanceof InstallPlanStep.PackageInstallStep) {
        pkg = ((InstallPlanStep.PackageInstallStep) step).getPackage();
        iconProperty.setValue(FontAwesome.DOWNLOAD);
      } else if (step instanceof InstallPlanStep.PackageUninstallStep) {
        pkg = ((InstallPlanStep.PackageUninstallStep) step).getInstalledPackage();
        iconProperty.setValue(FontAwesome.TRASH_O);
      } else if (step instanceof InstallPlanStep.PackageVersionChangeStep) {
        pkg = ((InstallPlanStep.PackageVersionChangeStep) step).getTargetPackage();
        final Package installedPackage = ((InstallPlanStep.PackageVersionChangeStep) step).getInstalledPackage();
        installedVersionProperty.setValue(installedPackage.getVersion().toString());

        if (installedPackage.getVersion().compareTo(pkg.getVersion()) < 0) {
          iconProperty.setValue(FontAwesome.ARROW_CIRCLE_O_UP);
        } else {
          iconProperty.setValue(FontAwesome.ARROW_CIRCLE_O_DOWN);
        }


      } else {
        LOG.error("Unsupported type of Install Plan Step:" + step);
        continue;
      }
      packageNameProperty.setValue(pkg.getName());
      packageVersionProperty.setValue(pkg.getVersion().toString());
    }

    // conflicts
    Table conflictsTable = tables.get(TableTypes.CONFLICTS);
    if (conflictsTable != null) {
      Container confictsTableDataSource = conflictsTable.getContainerDataSource();
      confictsTableDataSource.removeAllItems();
      for (PackageConflict conflict : packageManagerOperation.getConflicts()) {

        final Item item = confictsTableDataSource.getItem(confictsTableDataSource.addItem());
        final Property<String> packageNameProperty = item.getItemProperty(PROPERTY_PACKAGE_NAME);
        final Property<String> packageVersionProperty = item.getItemProperty(PROPERTY_PACKAGE_VERSION);

        Package pkg = conflict.getConflicting();
        packageNameProperty.setValue(pkg.getName());
        packageVersionProperty.setValue(pkg.getVersion().toString());
      }
    }

    // unresolved dependencies
    Table unresolvedTable = tables.get(TableTypes.UNRESOVED);
    if (unresolvedTable != null) {
      Container unresolvedTableDataSource = unresolvedTable.getContainerDataSource();
      unresolvedTableDataSource.removeAllItems();

      for (UnresolvedDependency unresolvedDep : packageManagerOperation.getUnresolved()) {

        final Item item = unresolvedTableDataSource.getItem(unresolvedTableDataSource.addItem());
        final Property<String> packageNameProperty = item.getItemProperty(PROPERTY_PACKAGE_NAME);
        final Property<String> packageVersionProperty = item.getItemProperty(PROPERTY_PACKAGE_VERSION);

        Package pkg;
        if (packageManagerOperation.hasPackagesToUninstall()) {
          pkg = unresolvedDep.getSource();
        } else {
          pkg = getPackage(unresolvedDep.getMissing());
        }
        if (pkg != null) {
          packageNameProperty.setValue(pkg.getName());
          packageVersionProperty.setValue(pkg.getVersion().toString());
        }
      }
    }

    // suggested
    Table suggestedTable = tables.get(TableTypes.SUGGESTED);
    if (suggestedTable != null) {
      Container suggestedTableDataSource = suggestedTable.getContainerDataSource();
      suggestedTableDataSource.removeAllItems();
      for (Package pkg : packageManagerOperation.getSuggested()) {

        final Item item = suggestedTableDataSource.getItem(suggestedTableDataSource.addItem());
        final Property<String> packageNameProperty = item.getItemProperty(PROPERTY_PACKAGE_NAME);
        final Property<String> packageVersionProperty = item.getItemProperty(PROPERTY_PACKAGE_VERSION);

        packageNameProperty.setValue(pkg.getName());
        packageVersionProperty.setValue(pkg.getVersion().toString());
      }
    }

  }

  /**
   * Find package for PackageRefernce
   *
   * @param packageReference PackageReference if package matches
   * @return a Package or null
   */
  private Package getPackage(PackageReference packageReference) {

    List<Package> installableAndExistingPackages = concat(
            packageManager.getInstalledPackages().stream(),
            packageManager.getInstallablePackages().stream()
    ).collect(Collectors.toList());

    for (Package _package : installableAndExistingPackages) {
      if (packageReference.matches(_package)) {
        return _package;
      }
    }

    return null;
  }

  @Override
  protected void onProceed() {
    close();
    onInstallListeners.forEach(Runnable::run);
  }

  public void onInstallClicked(Runnable runnable) {
    onInstallListeners.add(runnable);
  }

  enum TableTypes {
    INSTALL_UNINSTALL,
    CONFLICTS,
    UNRESOVED,
    SUGGESTED
  }
}

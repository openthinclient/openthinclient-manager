package org.openthinclient.web.pkgmngr.ui;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
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

import java.util.*;
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

  private final Map<GridTypes, Grid<InstallationSummary>> tables;
  private final PackageManagerOperation packageManagerOperation;
  private final PackageManager packageManager;

  private MVerticalLayout content = null;

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

    this.content = content;

    final Label l = new Label(getHeadlineText());
    l.addStyleName(ValoTheme.LABEL_HUGE);
    l.addStyleName(ValoTheme.LABEL_COLORED);
    content.addComponent(l);

    // install/uninstall
    tables.put(GridTypes.INSTALL_UNINSTALL, createTable());
    content.addComponent(new Label(getActionButtonCaption() + mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_ITEMS)));
    content.addComponent(tables.get(GridTypes.INSTALL_UNINSTALL));

    // conflicts
    if (!packageManagerOperation.getConflicts().isEmpty()) {
      tables.put(GridTypes.CONFLICTS, createTable());
      content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_CONFLICTS)));
      content.addComponent(tables.get(GridTypes.CONFLICTS));
    }

    // unresolved dependency
    if (!packageManagerOperation.getUnresolved().isEmpty()) {
      tables.put(GridTypes.UNRESOVED, createTable());
      if (packageManagerOperation.hasPackagesToUninstall()) {
        content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_DEPENDING_PACKAGE)));
      } else {
        content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_UNRESOLVED)));
      }
      content.addComponent(tables.get(GridTypes.UNRESOVED));
    }

    // suggested
    if (!packageManagerOperation.getSuggested().isEmpty()) {
      tables.put(GridTypes.SUGGESTED, createTable());
      content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_SUGGESTED)));
      content.addComponent(tables.get(GridTypes.SUGGESTED));
    }
  }

  /**
   * Creates a table with datasource of IndexedContainer
   * @return the Grid for InstallationSummary
   */
  private Grid<InstallationSummary> createTable() {

    Grid<InstallationSummary> summary = new Grid<>();
    summary.setDataProvider(DataProvider.ofCollection(Collections.EMPTY_LIST));
    summary.setSelectionMode(Grid.SelectionMode.NONE);
    summary.addColumn(InstallationSummary::getPackageName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    summary.addColumn(InstallationSummary::getPackageVersion).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));

    summary.addStyleName(ValoTheme.TABLE_BORDERLESS);
    summary.addStyleName(ValoTheme.TABLE_NO_HEADER);
    summary.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
    summary.addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
    summary.setHeightMode(HeightMode.ROW);

    return summary;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void update() {

    // install/uninstall steps
    Grid<InstallationSummary> installTable = tables.get(GridTypes.INSTALL_UNINSTALL);
    List<InstallationSummary> installationSummaries = new ArrayList<>();
    for (InstallPlanStep step : packageManagerOperation.getInstallPlan().getSteps()) {

      InstallationSummary is = new InstallationSummary();
      final Package pkg;
      if (step instanceof InstallPlanStep.PackageInstallStep) {
        pkg = ((InstallPlanStep.PackageInstallStep) step).getPackage();
        is.setIcon(VaadinIcons.DOWNLOAD);
      } else if (step instanceof InstallPlanStep.PackageUninstallStep) {
        pkg = ((InstallPlanStep.PackageUninstallStep) step).getInstalledPackage();
        is.setIcon(VaadinIcons.TRASH);
      } else if (step instanceof InstallPlanStep.PackageVersionChangeStep) {
        pkg = ((InstallPlanStep.PackageVersionChangeStep) step).getTargetPackage();
        final Package installedPackage = ((InstallPlanStep.PackageVersionChangeStep) step).getInstalledPackage();
          is.setInstalledVersion(installedPackage.getVersion().toString());

        if (installedPackage.getVersion().compareTo(pkg.getVersion()) < 0) {
          is.setIcon(VaadinIcons.ARROW_CIRCLE_UP_O);
        } else {
          is.setIcon(VaadinIcons.ARROW_CIRCLE_DOWN_O);
        }

      } else {
        LOG.error("Unsupported type of Install Plan Step:" + step);
        continue;
      }
      is.setPackageName(pkg.getName());
      is.setPackageVersion(pkg.getVersion().toString());
      installationSummaries.add(is);
    }
    installTable.setDataProvider(DataProvider.ofCollection(installationSummaries));
    setGridHeight(installTable, installationSummaries.size());


    // conflicts
    Grid<InstallationSummary> conflictsTable = tables.get(GridTypes.CONFLICTS);
    if (conflictsTable != null) {
      List<InstallationSummary> conflictsSummaries = new ArrayList<>();
      for (PackageConflict conflict : packageManagerOperation.getConflicts()) {
        Package pkg = conflict.getConflicting();
        conflictsSummaries.add(new InstallationSummary(pkg.getName(), pkg.getVersion().toString()));
      }
      conflictsTable.setDataProvider(DataProvider.ofCollection(conflictsSummaries));
    }

    // unresolved dependencies
    Grid<InstallationSummary> unresolvedTable = tables.get(GridTypes.UNRESOVED);
    if (unresolvedTable != null) {
      List<InstallationSummary> unresolvedSummaries = new ArrayList<>();
      for (UnresolvedDependency unresolvedDep : packageManagerOperation.getUnresolved()) {
        Package pkg;
        if (packageManagerOperation.hasPackagesToUninstall()) {
          pkg = unresolvedDep.getSource();
        } else {
          pkg = getPackage(unresolvedDep.getMissing());
        }
        if (pkg != null) {
          unresolvedSummaries.add(new InstallationSummary(pkg.getName(), pkg.getVersion().toString()));
        }
      }
      unresolvedTable.setDataProvider(DataProvider.ofCollection(unresolvedSummaries));
      setGridHeight(unresolvedTable, unresolvedSummaries.size());
    }

    // suggested
    Grid<InstallationSummary> suggestedTable = tables.get(GridTypes.SUGGESTED);
    if (suggestedTable != null) {
      suggestedTable.setDataProvider(DataProvider.ofCollection(
              packageManagerOperation.getSuggested().stream()
                                     .map(pkg -> new InstallationSummary(pkg.getName(), pkg.getVersion().toString()))
                                     .collect(Collectors.toList())
      ));
      setGridHeight(suggestedTable, packageManagerOperation.getSuggested().size());
    }
  }

  private void setGridHeight(Grid grid, int size) {
    // TODO: magic numbers
    grid.setWidth("100%");
    grid.setHeight(((size + 1) * 38.5) + "px");
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

  enum GridTypes {
    INSTALL_UNINSTALL,
    CONFLICTS,
    UNRESOVED,
    SUGGESTED
  }

  class InstallationSummary {

    private Resource icon;
    private Class propertyType;
    private String packageName;
    private String packageVersion;
    private String installedVersion;

    public InstallationSummary() {}

    public InstallationSummary(String pkgName, String packageVersion) {
      this.packageName = pkgName;
      this.packageVersion = packageVersion;
    }

    public Resource getIcon() {
      return icon;
    }

    public void setIcon(Resource icon) {
      this.icon = icon;
    }

    public Class getPropertyType() {
      return propertyType;
    }

    public void setPropertyType(Class propertyType) {
      this.propertyType = propertyType;
    }

    public String getPackageName() {
      return packageName;
    }

    public void setPackageName(String packageName) {
      this.packageName = packageName;
    }

    public String getPackageVersion() {
      return packageVersion;
    }

    public void setPackageVersion(String packageVersion) {
      this.packageVersion = packageVersion;
    }

    public String getInstalledVersion() {
      return installedVersion;
    }

    public void setInstalledVersion(String installedVersion) {
      this.installedVersion = installedVersion;
    }
  }
}

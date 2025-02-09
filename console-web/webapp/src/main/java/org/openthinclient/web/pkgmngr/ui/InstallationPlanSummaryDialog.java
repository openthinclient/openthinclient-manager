package org.openthinclient.web.pkgmngr.ui;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.*;
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
import org.springframework.context.ApplicationContext;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Stream.concat;
import static org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter.HideOTCManagerVersionFilter.OPENTHINCLIENT_MANAGER_VERSION_NAME;

public class InstallationPlanSummaryDialog extends AbstractSummaryDialog {
  public static final String PROPERTY_TYPE = "type";
  public static final String PROPERTY_PACKAGE_NAME = "packageName";
  public static final String PROPERTY_PACKAGE_VERSION = "packageVersion";
  public static final String PROPERTY_INSTALLED_VERSION = "newVersion";
  private static final Logger LOG = LoggerFactory.getLogger(InstallationPlanSummaryDialog.class);

  private final List<Runnable> onInstallListeners;

  private final Map<GridTypes, Grid<InstallationSummary>> tables;
  private final PackageManagerOperation packageManagerOperation;
  private final PackageManager packageManager;
  private final ApplicationContext applicationContext;
  private final CheckBox licenseAgreementCheckBox = new CheckBox();
  private final TextArea licenseTextArea = new TextArea();
  private final List<Button> licenseButtons = new ArrayList<>();

  private AbstractLayout updateServerHint;

  public InstallationPlanSummaryDialog(PackageManagerOperation packageManagerOperation, PackageManager packageManager, ApplicationContext applicationContext) {
    super();

    this.packageManager = packageManager;
    this.packageManagerOperation = packageManagerOperation;
    this.applicationContext = applicationContext;
    tables = new HashMap<>();

    proceedButton.setCaption(getActionButtonCaption());
    // prevent install/uninstall if there are unresolved dependencies
    proceedButton.setEnabled(packageManagerOperation.getUnresolved().isEmpty() && packageManagerOperation.getConflicts().isEmpty());

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

    // workaround for vaadin's vanishing scrollbars
    content.setStyleName("installationplansummary");

    final Label l = new Label(getHeadlineText());
    l.addStyleName(ValoTheme.LABEL_HUGE);
    l.addStyleName(ValoTheme.LABEL_COLORED);
    content.addComponent(l);

    // install/uninstall
    tables.put(GridTypes.INSTALL_UNINSTALL, createTable(GridTypes.INSTALL_UNINSTALL));
    content.addComponent(new Label(getActionButtonCaption() + " " + mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_ITEMS)));
    content.addComponent(tables.get(GridTypes.INSTALL_UNINSTALL));

    // conflicts
    if (!packageManagerOperation.getConflicts().isEmpty()) {
      tables.put(GridTypes.CONFLICTS, createTable(GridTypes.CONFLICTS));
      content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_CONFLICTS)));
      content.addComponent(tables.get(GridTypes.CONFLICTS));
    }

    // unresolved dependency
    if (!packageManagerOperation.getUnresolved().isEmpty()) {
      tables.put(GridTypes.UNRESOLVED, createTable(GridTypes.UNRESOLVED));
      if (packageManagerOperation.hasPackagesToUninstall()) {
        content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_DEPENDING_PACKAGE)));
      } else {
        content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_UNRESOLVED)));
      }
      content.addComponent(tables.get(GridTypes.UNRESOLVED));
    }

    // suggested
    if (!packageManagerOperation.getSuggested().isEmpty()) {
      tables.put(GridTypes.SUGGESTED, createTable(GridTypes.SUGGESTED));
      content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_SUGGESTED)));
      content.addComponent(tables.get(GridTypes.SUGGESTED));
    }

    // license
    licenseTextArea.addStyleNames("otc-content-wrap", "license-area");
    licenseTextArea.setWidth(100, Unit.PERCENTAGE);
    licenseTextArea.setVisible(false);
    content.add(licenseTextArea);

    if (containsLicenseAgreement()) {
      content.addComponent(new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_INSTALLATIONPLAN_LICENSE_CAPTION)));
      licenseAgreementCheckBox.setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_DETAILS_LICENSE_CHECKBOX_CAPTION));
      if (proceedButton.isEnabled()) {
        proceedButton.setEnabled(false);
        licenseAgreementCheckBox.addValueChangeListener(e -> proceedButton.setEnabled(e.getValue()));
      } else {
        licenseAgreementCheckBox.setEnabled(false);
      }
      content.addComponent(licenseAgreementCheckBox);
    }
    boolean isPreview = applicationContext.getEnvironment().getProperty("application.is-preview", Boolean.class);
    if (!isPreview && containsPreviewInstallation()) {
      Label warning = new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PREVIEW_WARNING), ContentMode.HTML);
      warning.setStyleName("preview-warning");
      content.addComponent(warning);
    }

    // Update to new OTC-manager hint
    updateServerHint = new CssLayout();
    updateServerHint.addStyleName("update-server-hint");
    updateServerHint.setVisible(false);
    Label label = new Label(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_MANAGER_TOO_OLD));
    label.setContentMode(ContentMode.HTML);
    Button link = new Button(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_MANAGER_TOO_OLD_CHECK_BUTTON));
    link.addClickListener((e) -> {
      UI.getCurrent().getNavigator().navigateTo("support");
      UI.getCurrent().getWindows().forEach(UI.getCurrent()::removeWindow);
    });
    updateServerHint.addComponents(label, link);
    content.add(updateServerHint);

  }

  /**
   * Creates a table with datasource of IndexedContainer
   * @return the Grid for InstallationSummary
   */
  private Grid<InstallationSummary> createTable(GridTypes type) {

    Grid<InstallationSummary> summary = new Grid<>();
    summary.setDataProvider(DataProvider.ofCollection(Collections.EMPTY_LIST));
    summary.setSelectionMode(Grid.SelectionMode.NONE);
    summary.addColumn(InstallationSummary::getPackageName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    summary.addColumn(InstallationSummary::getPackageVersion).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
    if (type == GridTypes.INSTALL_UNINSTALL && !packageManagerOperation.hasPackagesToUninstall()) { // license column
      summary.addComponentColumn(is -> {
        if (is.getLicense() != null) {
          Button button = new Button(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_LICENSE_SHOW));
          button.addClickListener(click -> {
                // license already clicked, re-set button caption
                licenseButtons.stream().filter(b -> !b.equals(button)).forEach(b -> {
                  if (b.getCaption().equals(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_LICENSE_HIDE))) {
                    b.setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_LICENSE_SHOW));
                  }
                });
                // display license
                if (button.getCaption().equals(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_LICENSE_SHOW))) {
                  licenseTextArea.setVisible(true);
                  licenseTextArea.setValue(is.getLicense());
                } else {
                  licenseTextArea.setVisible(false);
                }
                button.setCaption(licenseTextArea.isVisible() ? mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_LICENSE_HIDE)
                                                              : mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_LICENSE_SHOW));
              }
          );
          button.addStyleName("package_install_summary_display_license_button");
          licenseButtons.add(button);
          return button;
        } else {
          return null;
        }
      }).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_LICENSE));
    }

    summary.addStyleName(ValoTheme.TABLE_BORDERLESS);
    summary.addStyleName(ValoTheme.TABLE_NO_HEADER);
    summary.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
    summary.addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
    summary.setHeightMode(HeightMode.ROW);
    summary.addStyleName("scrollable");

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
        is.setInstalledVersion(installedPackage.getVersion().toStringWithoutEpoch());

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
      is.setPackageVersion(pkg.getVersion().toStringWithoutEpoch());
      is.setLicense(pkg.getLicense());
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
        // prevent duplicate entries in list
        Optional<InstallationSummary> any = conflictsSummaries.stream().filter(is -> is.packageName.equals(pkg.getName()) && is.packageVersion.equals(pkg.getVersion().toString())).findAny();
        if (!any.isPresent()) {
          conflictsSummaries.add(new InstallationSummary(pkg.getName(), pkg.getVersion() != null ? pkg.getDisplayVersion() : "", pkg.getLicense()));
        }

        // show OTC-server version update  hint
        if (pkg.getName().equals(OPENTHINCLIENT_MANAGER_VERSION_NAME)) {
          updateServerHint.setVisible(true);
        }
      }
      conflictsTable.setDataProvider(DataProvider.ofCollection(conflictsSummaries));
      setGridHeight(conflictsTable, conflictsSummaries.size());
    }

    // unresolved dependencies
    Grid<InstallationSummary> unresolvedTable = tables.get(GridTypes.UNRESOLVED);
    if (unresolvedTable != null) {
      List<InstallationSummary> unresolvedSummaries = new ArrayList<>();
      for (UnresolvedDependency unresolvedDep : packageManagerOperation.getUnresolved()) {
        Package pkg;
        if (packageManagerOperation.hasPackagesToUninstall()) {
          pkg = unresolvedDep.getSource();
          if (pkg != null) {
            unresolvedSummaries.add(new InstallationSummary(pkg.getName(), pkg.getVersion() != null ? pkg.getDisplayVersion(): "", pkg.getLicense()));
          }
        } else {
          if (unresolvedDep.getMissing() instanceof PackageReference.SingleReference) {
            PackageReference.SingleReference missing = (PackageReference.SingleReference) unresolvedDep.getMissing();
            unresolvedSummaries.add(new InstallationSummary(missing.getName(), missing.getVersion() != null ? missing.getVersion().toStringWithoutEpoch() : "", null));

            // show OTC-server version update  hint
            if (missing.getName().equals(OPENTHINCLIENT_MANAGER_VERSION_NAME)) {
              updateServerHint.setVisible(true);
            }
          }
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
                                     .map(pkg -> new InstallationSummary(pkg.getName(), pkg.getVersion().toStringWithoutEpoch(), pkg.getLicense()))
                                     .collect(Collectors.toList())
      ));
      setGridHeight(suggestedTable, packageManagerOperation.getSuggested().size());
    }
  }

  /**
   * Check, if there is at least on package with a license-agreement
   * @return {@code true} if a package with a license has been found
   */
  private boolean containsLicenseAgreement() {
    for (InstallPlanStep step : packageManagerOperation.getInstallPlan().getSteps()) {
      final Package pkg;
      if (step instanceof InstallPlanStep.PackageInstallStep) {
        pkg = ((InstallPlanStep.PackageInstallStep) step).getPackage();
      } else if (step instanceof InstallPlanStep.PackageVersionChangeStep) {
        pkg = ((InstallPlanStep.PackageVersionChangeStep) step).getTargetPackage();
      } else {
        continue;
      }
      if (pkg.getLicense() != null) {
        return true;
      }
    }
    return false;
  }

  private boolean containsPreviewInstallation() {
    for (InstallPlanStep step : packageManagerOperation.getInstallPlan().getSteps()) {
      if (step instanceof InstallPlanStep.PackageInstallStep) {
        return ((InstallPlanStep.PackageInstallStep) step).getPackage().getVersion().isPreview();
      } else if (step instanceof InstallPlanStep.PackageVersionChangeStep) {
        InstallPlanStep.PackageVersionChangeStep changeStep = ((InstallPlanStep.PackageVersionChangeStep) step);
        return (changeStep.getTargetPackage().getVersion().isPreview()
                 && !changeStep.getInstalledPackage().getVersion().isPreview());
      } else {
        continue;
      }
    }
    return false;
  }

  private void setGridHeight(Grid grid, int size) {
    grid.setWidth("100%");
    if (size == 0)
      // FIXME in case of an empty grid, the grid should be omitted and a "Nothing to see here" message should be displayed.
      // Right now only a empty grid is displayed to the user. The height of 39 is the height of the grid header
      grid.setHeight(39, Sizeable.Unit.PIXELS);
    else
      grid.setHeightByRows(size);
  }

  /**
   * Find package for PackageReference
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
    UNRESOLVED,
    SUGGESTED
  }

  class InstallationSummary {

    private Resource icon;
    private Class propertyType;
    private String packageName;
    private String packageVersion;
    private String installedVersion;

    private String license;

    public InstallationSummary() {}

    public InstallationSummary(String pkgName, String packageVersion, String license) {
      this.packageName = pkgName;
      this.packageVersion = packageVersion;
      this.license = license;
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

    public String getLicense() {
      return license;
    }

    public void setLicense(String license) {
      this.license = license;
    }
  }
}

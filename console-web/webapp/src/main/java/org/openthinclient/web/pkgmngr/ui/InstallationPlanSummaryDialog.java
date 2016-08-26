package org.openthinclient.web.pkgmngr.ui;

import static java.util.stream.Stream.concat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.InstallPlanStep;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.PackageConflict;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.UnresolvedDependency;
import org.openthinclient.util.dpkg.PackageReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class InstallationPlanSummaryDialog {
    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_PACKAGE_NAME = "packageName";
    public static final String PROPERTY_PACKAGE_VERSION = "packageVersion";
    public static final String PROPERTY_INSTALLED_VERSION = "newVersion";
    private static final Logger LOG = LoggerFactory.getLogger(InstallationPlanSummaryDialog.class);
    private static final String PROPERTY_ICON = "icon";
    private final Window window;
    private final MHorizontalLayout footer;
    private final MButton cancelButton;
    private final MButton installButton;

    private final List<Runnable> onInstallListeners;

    private Map<TableTypes, Table> tables;
    private PackageManagerOperation packageManagerOperation;
    private PackageManager packageManager;

    public InstallationPlanSummaryDialog(PackageManagerOperation packageManagerOperation, PackageManager packageManager) {
      
        this.packageManager = packageManager;
        this.packageManagerOperation = packageManagerOperation;
        tables = new HashMap<>();
      
        String headlineText = "Installation";
        String actionButtonCaption = "Install";
        if (packageManagerOperation.hasPackagesToUninstall()) {
          headlineText = "Unistallation";
          actionButtonCaption = "Uninstall";
        }
      
        window = new Window();
        window.setWidth(60, Sizeable.Unit.PERCENTAGE);
        window.setHeight(null);
        window.center();

        final MVerticalLayout content = new MVerticalLayout()
                .withMargin(true)
                .withSpacing(true);

        final Label l = new Label(headlineText);
        l.addStyleName(ValoTheme.LABEL_HUGE);
        l.addStyleName(ValoTheme.LABEL_COLORED);
        content.addComponent(l);

        // install/uninstall
        tables.put(TableTypes.INSTALL_UNINSTALL, createTable());
        content.addComponent(new Label(actionButtonCaption + " items:"));
        content.addComponent(tables.get(TableTypes.INSTALL_UNINSTALL));
        
        // conflicts
        if (!packageManagerOperation.getConflicts().isEmpty()) {
          tables.put(TableTypes.CONFLICTS, createTable());
          content.addComponent(new Label("Conflicts:"));
          content.addComponent(tables.get(TableTypes.CONFLICTS));        
        }
        
        // unresolved dependency
        if (!packageManagerOperation.getUnresolved().isEmpty()) {
          tables.put(TableTypes.UNRESOVED, createTable());
          if (packageManagerOperation.hasPackagesToUninstall()) {
            content.addComponent(new Label("Packages depends on package to uninstall:"));
          } else {
            content.addComponent(new Label("Unresolved dependency (missing packages):"));
          }
          content.addComponent(tables.get(TableTypes.UNRESOVED));        
        }
        
        // suggested
        if (!packageManagerOperation.getSuggested().isEmpty()) {
          tables.put(TableTypes.SUGGESTED, createTable());
          content.addComponent(new Label("Suggested:"));
          content.addComponent(tables.get(TableTypes.SUGGESTED));        
        }        
        

        // controls
        installButton = new MButton(actionButtonCaption).withStyleName(ValoTheme.BUTTON_PRIMARY).withListener(e -> doInstall());
//         installButton.setEnabled(!packageManagerOperation.getInstallPlan().getSteps().isEmpty());
        // prevent install/unistall if there are unresolved dependencies
        installButton.setEnabled(packageManagerOperation.getUnresolved().isEmpty());
        
        cancelButton = new MButton("Cancel").withListener(e -> close());
        footer = new MHorizontalLayout()
                .withFullWidth()
                .withStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR)
                .with(installButton, cancelButton);
        footer.setComponentAlignment(installButton, Alignment.TOP_RIGHT);
        footer.setExpandRatio(installButton, 1);
        content.addComponent(footer);

        window.setContent(content);

        onInstallListeners = new ArrayList<>(2);

        update();
    }

    /**
     * Creates a table with datasource of IndexedContainer 
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

      table.setWidth("100%");
      table.setHeight(100, Sizeable.Unit.PIXELS);
      
      return table;
    }

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
                installedVersionProperty.setValue(((InstallPlanStep.PackageVersionChangeStep) step).getInstalledPackage().getVersion().toString());
                iconProperty.setValue(FontAwesome.REFRESH);
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
      };
      
      return null;
    }

    private void doInstall() {
        close();
        onInstallListeners.forEach(Runnable::run);
    }

    public void onInstallClicked(Runnable runnable) {
        onInstallListeners.add(runnable);
    }

    public void open(boolean modal) {
        window.setModal(modal);
        if (!isOpen())
            UI.getCurrent().addWindow(window);
    }

    public boolean isOpen() {
        return UI.getCurrent().getWindows().contains(window);
    }

    public void close() {
        UI.getCurrent().removeWindow(window);
    }
    
    enum TableTypes {
      INSTALL_UNINSTALL,
      CONFLICTS,
      UNRESOVED,
      SUGGESTED
    }
}

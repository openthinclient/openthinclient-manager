package org.openthinclient.web.pkgmngr.ui;

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

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.InstallPlan;
import org.openthinclient.pkgmgr.op.InstallPlanStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.util.ArrayList;
import java.util.List;

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
    private final InstallPlan installPlan;
    private final Table table;
    private final IndexedContainer container;
    private final List<Runnable> onInstallListeners;

    public InstallationPlanSummaryDialog(InstallPlan installPlan) {
      this(installPlan, true);
    }
    
    public InstallationPlanSummaryDialog(InstallPlan installPlan, boolean install) {
      
        String headlineText = "Installation";
        String actionButtonCaption = "Install";
        if (!install) {
          headlineText = "Unistallation";
          actionButtonCaption = "Uninstall";
        }
      
        this.installPlan = installPlan;
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

        container = new IndexedContainer();
        container.addContainerProperty(PROPERTY_ICON, Resource.class, null);
        container.addContainerProperty(PROPERTY_TYPE, Class.class, null);
        container.addContainerProperty(PROPERTY_PACKAGE_NAME, String.class, null);
        container.addContainerProperty(PROPERTY_PACKAGE_VERSION, String.class, null);
        container.addContainerProperty(PROPERTY_INSTALLED_VERSION, String.class, "");

        table = new Table();
        table.addStyleName(ValoTheme.TABLE_BORDERLESS);
        table.addStyleName(ValoTheme.TABLE_NO_HEADER);
        table.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        table.addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        table.setContainerDataSource(container);
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
        table.setHeight(200, Sizeable.Unit.PIXELS);

        content.addComponent(table);

        installButton = new MButton(actionButtonCaption).withStyleName(ValoTheme.BUTTON_PRIMARY).withListener(e -> doInstall());
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

    @SuppressWarnings("unchecked")
    public void update() {
        container.removeAllItems();

        for (InstallPlanStep step : installPlan.getSteps()) {
            final Item item = container.getItem(container.addItem());
            final Property iconProperty = item.getItemProperty(PROPERTY_ICON);
            final Property installedVersionProperty = item.getItemProperty(PROPERTY_INSTALLED_VERSION);
            final Property packageNameProperty = item.getItemProperty(PROPERTY_PACKAGE_NAME);
            final Property packageVersionProperty = item.getItemProperty(PROPERTY_PACKAGE_VERSION);

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
}

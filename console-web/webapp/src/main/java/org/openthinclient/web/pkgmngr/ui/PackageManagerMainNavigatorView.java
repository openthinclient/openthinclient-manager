package org.openthinclient.web.pkgmngr.ui;

import java.util.Collection;
import java.util.concurrent.Callable;

import javax.annotation.PreDestroy;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine.Registration;
import static org.openthinclient.web.i18n.ConsoleWebMessages.*;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsListPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.view.PackageListMasterDetailsView;
import org.openthinclient.web.pkgmngr.ui.view.PackageManagerMainView;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

@SpringView(name = "package-management")
@SideBarItem(sectionId = DashboardSections.PACKAGE_MANAGEMENT, captionCode = "UI_PACKAGEMANAGERMAINNAVIGATORVIEW_CAPTION")
public class PackageManagerMainNavigatorView extends Panel implements View {

    /** serialVersionUID  */
    private static final long serialVersionUID = -1596921762830560217L;
    /** LOGGER */
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageManagerMainNavigatorView.class);
    
    private final VerticalLayout root;
    private final PackageListMasterDetailsPresenter availablePackagesPresenter;
    private final PackageListMasterDetailsPresenter installedPackagesPresenter;
    private final PackageManager packageManager;
    
    private Registration handler;
    
    @Autowired
    public PackageManagerMainNavigatorView(final PackageManager packageManager, 
                                           final PackageManagerExecutionEngine packageManagerExecutionEngine) {
        this.packageManager = packageManager;
        
        final IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
        
        addStyleName(ValoTheme.PANEL_BORDERLESS);
        setSizeFull();

        final PackageManagerMainView mainView = new PackageManagerMainView();
        
        mainView.setTabCaption(mainView.getAvailablePackagesView(), mc.getMessage(UI_PACKAGEMANAGER_TAB_AVAILABLEPACKAGES));
        mainView.setTabCaption(mainView.getInstalledPackagesView(), mc.getMessage(UI_PACKAGEMANAGER_TAB_INSTALLEDPACKAGES));
        
        this.availablePackagesPresenter = createPresenter(mainView.getAvailablePackagesView());
        this.installedPackagesPresenter = createPresenter(mainView.getInstalledPackagesView());

        root = new VerticalLayout();
        root.setSizeFull();
        root.setMargin(true);
        setContent(root);

        root.addComponent(new ViewHeader(mc.getMessage(UI_PACKAGEMANAGERMAINNAVIGATORVIEW_CAPTION)));

        root.addComponent(buildSparklines());

        root.addComponent(mainView);
        root.setExpandRatio(mainView, 1);
        
        handler = packageManagerExecutionEngine.addTaskFinalizedHandler(e -> {
          bindPackageList(PackageManagerMainNavigatorView.this.availablePackagesPresenter, packageManager::getInstallablePackages);
          bindPackageList(PackageManagerMainNavigatorView.this.installedPackagesPresenter, packageManager::getInstalledPackages);
        });
        
    }

    private Component buildSparklines() {
        CssLayout sparks = new CssLayout();
        sparks.addStyleName("sparks");
        sparks.setWidth("100%");
        Responsive.makeResponsive(sparks);
        return sparks;
    }


    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        bindPackageList(this.availablePackagesPresenter, packageManager::getInstallablePackages);
        bindPackageList(this.installedPackagesPresenter, packageManager::getInstalledPackages);
    }

    private PackageListMasterDetailsPresenter createPresenter(PackageListMasterDetailsView masterDetailsView) {
        return new PackageListMasterDetailsPresenter(masterDetailsView, new PackageDetailsListPresenter(masterDetailsView.getPackageDetailsView(), packageManager));
    }

    private void bindPackageList(PackageListMasterDetailsPresenter presenter, Callable<Collection<Package>> packagesProvider) {
        try {
            presenter.setPackages(packagesProvider.call());
        } catch (Exception e) {
            presenter.showPackageListLoadingError(e);
            // FIXME
            e.printStackTrace();
        }

    }

    @PreDestroy
    public void cleanup() {
      LOGGER.debug("Cleaup {} and unregister {}", this, handler);
      handler.unregister();
    }

}

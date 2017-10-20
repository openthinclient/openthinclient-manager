package org.openthinclient.web.pkgmngr.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine.Registration;
import org.openthinclient.web.SchemaService;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsListPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.view.PackageListMasterDetailsView;
import org.openthinclient.web.pkgmngr.ui.view.PackageManagerMainView;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.concurrent.Callable;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

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
    private final PackageListMasterDetailsPresenter updateablePackagesPresenter;
    private final PackageManager packageManager;
    private final SchemaService schemaService;
    private final PackageManagerMainView mainView;

    private final Registration handler;
    private final ApplicationService applicationService;
    @Autowired
    private EventBus.SessionEventBus eventBus;

    @Autowired
    public PackageManagerMainNavigatorView(final PackageManager packageManager,
                                           final PackageManagerExecutionEngine packageManagerExecutionEngine,
                                           final SchemaService schemaService, ApplicationService applicationService) {
        this.packageManager = packageManager;
        this.schemaService = schemaService;
        this.applicationService = applicationService;

        final IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

        addStyleName(ValoTheme.PANEL_BORDERLESS);
        setSizeFull();

        root = new VerticalLayout();
        root.setSizeFull();
        root.setMargin(true);
        root.addStyleName("dashboard-view");
        setContent(root);
        Responsive.makeResponsive(root);

        root.addComponent(new ViewHeader(mc.getMessage(UI_PACKAGEMANAGERMAINNAVIGATORVIEW_CAPTION)));

        mainView = new PackageManagerMainView();
        mainView.setTabCaption(mainView.getAvailablePackagesView(), mc.getMessage(UI_PACKAGEMANAGER_TAB_AVAILABLEPACKAGES));
        mainView.setTabCaption(mainView.getUpdateablePackagesView(), mc.getMessage(UI_PACKAGEMANAGER_TAB_UPDATEABLEPACKAGES));
        mainView.setTabCaption(mainView.getInstalledPackagesView(), mc.getMessage(UI_PACKAGEMANAGER_TAB_INSTALLEDPACKAGES));
        
        this.availablePackagesPresenter = createPresenter(mainView.getAvailablePackagesView());
        this.updateablePackagesPresenter = createPresenter(mainView.getUpdateablePackagesView());
        this.installedPackagesPresenter = createPresenter(mainView.getInstalledPackagesView());

        // in case of the installed packages, there must never be any package with different versions. Due to this,
        // filtering is not useful and the option should not be presented to the user.
        this.installedPackagesPresenter.setVersionFilteringAllowed(false);
        this.updateablePackagesPresenter.setVersionFilteringAllowed(false);

        root.addComponent(mainView);
        root.setExpandRatio(mainView, 1);
        
        handler = packageManagerExecutionEngine.addTaskFinalizedHandler(e -> {
          bindPackageList(PackageManagerMainNavigatorView.this.availablePackagesPresenter, packageManager::getInstallablePackages);
          bindPackageList(PackageManagerMainNavigatorView.this.installedPackagesPresenter, packageManager::getInstalledPackages);
          bindPackageList(PackageManagerMainNavigatorView.this.updateablePackagesPresenter, packageManager::getUpdateablePackages);
        });

    }

    @PostConstruct
    public void init() {
        eventBus.subscribe(this);
    }

    @EventBusListenerMethod
    public void windowResize(final DashboardEvent.BrowserResizeEvent event) {
        setGritHeight(event.getHeight(), mainView.getUpdateablePackagesView().getPackageList());
        setGritHeight(event.getHeight(), mainView.getInstalledPackagesView().getPackageList());
        setGritHeight(event.getHeight(), mainView.getAvailablePackagesView().getPackageList());
    }

    private void setGritHeight(int windowHeight , Grid<?> grid) {
        windowHeight = windowHeight - 270;
        int height = (((ListDataProvider<Object>) grid.getDataProvider()).getItems().size() + 1) * 38 + 2;
        if (height > windowHeight) {
            height = windowHeight;
        }
        grid.setHeight(height, Unit.PIXELS);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        bindPackageList(this.availablePackagesPresenter, packageManager::getInstallablePackagesWithoutInstalledOfSameVersion);
        bindPackageList(this.installedPackagesPresenter, packageManager::getInstalledPackages);
        bindPackageList(this.updateablePackagesPresenter, packageManager::getUpdateablePackages);

        setGritHeight(Page.getCurrent().getBrowserWindowHeight(), mainView.getInstalledPackagesView().getPackageList());
        setGritHeight(Page.getCurrent().getBrowserWindowHeight(), mainView.getAvailablePackagesView().getPackageList());
        setGritHeight(Page.getCurrent().getBrowserWindowHeight(), mainView.getUpdateablePackagesView().getPackageList());
    }

    private PackageListMasterDetailsPresenter createPresenter(PackageListMasterDetailsView masterDetailsView) {
        return new PackageListMasterDetailsPresenter(masterDetailsView, new PackageDetailsListPresenter(masterDetailsView.getPackageDetailsView(), packageManager, schemaService, applicationService));
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

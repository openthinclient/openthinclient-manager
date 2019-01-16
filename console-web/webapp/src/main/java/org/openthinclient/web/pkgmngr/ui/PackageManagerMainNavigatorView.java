package org.openthinclient.web.pkgmngr.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;

import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.openthinclient.progress.Registration;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.SchemaService;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.pkgmngr.ui.presenter.AvailablePackageListMasterDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageActionOverviewPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsListPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.UpdateablePackageListMasterDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.view.PackageActionOverviewView;
import org.openthinclient.web.pkgmngr.ui.view.PackageListMasterDetailsView;
import org.openthinclient.web.pkgmngr.ui.view.PackageManagerMainView;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SpringView(name = "package-management")
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_PACKAGEMANAGERMAINNAVIGATORVIEW_CAPTION", order = 1)
@ThemeIcon("icon/package-white.svg")
public class PackageManagerMainNavigatorView extends Panel implements View {

  private static final long serialVersionUID = -1596921762830560217L;
  private static final Logger LOGGER = LoggerFactory.getLogger(PackageManagerMainNavigatorView.class);

  private final PackageManagerMainView mainView;
  private final PackageListMasterDetailsPresenter availablePackagesPresenter;
  private final PackageListMasterDetailsPresenter installedPackagesPresenter;
  private final PackageListMasterDetailsPresenter updateablePackagesPresenter;
  private final PackageManager packageManager;
  private final SchemaService schemaService;
  private final ClientService clientService;

  private final Registration handler;
  private final ApplicationService applicationService;
  private final IMessageConveyor mc;

  @Autowired
  public PackageManagerMainNavigatorView(final PackageManager packageManager,
                                         final PackageManagerExecutionEngine packageManagerExecutionEngine,
                                         final SchemaService schemaService, ApplicationService applicationService,
                                         final ClientService clientService,
                                         final EventBus.SessionEventBus eventBus,
                                         final DashboardNotificationService notificationService) {

    this.packageManager = packageManager;
    this.schemaService = schemaService;
    this.applicationService = applicationService;
    this.clientService = clientService;
    this.mc = new MessageConveyor(UI.getCurrent().getLocale());
    this.mainView = new PackageManagerMainView();

    this.availablePackagesPresenter = createPresenter(PackageDetailsListPresenter.Mode.INSTALL, mainView.getAvailablePackagesView());
    this.updateablePackagesPresenter = createPresenter(PackageDetailsListPresenter.Mode.UPDATE, mainView.getUpdateablePackagesView());
    this.installedPackagesPresenter = createPresenter(PackageDetailsListPresenter.Mode.UNINSTALL, mainView.getInstalledPackagesView());

    setSizeFull();
    eventBus.publish(this, new DashboardEvent.UpdateHeaderLabelEvent(mc.getMessage(UI_PACKAGEMANAGERMAINNAVIGATORVIEW_CAPTION)));

    handler = packageManagerExecutionEngine.addTaskFinalizedHandler(e -> {
      bindPackageLists();
    });

  }

  @PostConstruct
  private void init() {
    addStyleName("package-manager");
    setContent(buildContent());
  }

  private Component buildContent() {
    mainView.setTabCaption(mainView.getAvailablePackagesView(), mc.getMessage(UI_PACKAGEMANAGER_TAB_AVAILABLEPACKAGES));
    mainView.setTabCaption(mainView.getUpdateablePackagesView(), mc.getMessage(UI_PACKAGEMANAGER_TAB_UPDATEABLEPACKAGES));
    mainView.setTabCaption(mainView.getInstalledPackagesView(), mc.getMessage(UI_PACKAGEMANAGER_TAB_INSTALLEDPACKAGES));
    mainView.addSelectedTabChangeListener(event -> {
          PackageManagerMainNavigatorView.this.updateablePackagesPresenter.refreshUpdatePanel();
          PackageManagerMainNavigatorView.this.availablePackagesPresenter.refreshUpdatePanel();
    });
    // handle sourceUpdatePanel-view
    mainView.getInstalledPackagesView().hideSourceUpdatePanel();
    return mainView;
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    bindPackageLists();
  }

  private void bindPackageLists() {
    bindPackageList(this.availablePackagesPresenter, packageManager::getInstallablePackagesWithoutInstalledOfSameVersion);
    bindPackageList(this.installedPackagesPresenter, packageManager::getInstalledPackages);
    bindPackageList(this.updateablePackagesPresenter, packageManager::getUpdateablePackages);
  }

  private PackageListMasterDetailsPresenter createPresenter(PackageDetailsListPresenter.Mode mode, PackageListMasterDetailsView masterDetailsView) {

    final PackageActionOverviewView packageActionOverviewView = new PackageActionOverviewView();
    masterDetailsView.getDetailsContainer().addComponent(packageActionOverviewView);

    final PackageDetailsListPresenter packageDetailsListPresenter = new PackageDetailsListPresenter(mode, new PackageActionOverviewPresenter(packageActionOverviewView), packageManager, schemaService, applicationService, clientService);

    Consumer<Collection<Package>> presenter = packageDetailsListPresenter::setPackages;
    if (mode == PackageDetailsListPresenter.Mode.INSTALL) {
      // in case of the installation mode, we're using the AvailablePackageListMasterDetailsPresenter
      // as it does some additional filtering of the package list, specific to the handling of
      // packages that may be installed
      return new AvailablePackageListMasterDetailsPresenter(masterDetailsView, presenter, packageManager, clientService);
    }
    if (mode == PackageDetailsListPresenter.Mode.UPDATE) {
      // similar to the installation mode, update has some specific behaviour that will be handled
      // using the following Presenter
      return new UpdateablePackageListMasterDetailsPresenter(masterDetailsView, presenter, packageManager, clientService);
    }
    return new PackageListMasterDetailsPresenter(masterDetailsView, presenter, packageManager, clientService);
  }

  private void bindPackageList(PackageListMasterDetailsPresenter presenter, Callable<Collection<Package>> packagesProvider) {
    try {
      presenter.setPackages(packagesProvider.call());
    } catch (Exception e) {
      presenter.showPackageListLoadingError(e);
      LOGGER.error("Failed to load package list", e);
    }

  }

  @PreDestroy
  public void cleanup() {
    LOGGER.debug("Cleanup {} and unregister {}", this, handler);
    handler.unregister();
  }

}

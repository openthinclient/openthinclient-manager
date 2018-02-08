package org.openthinclient.web.pkgmngr.ui;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.openthinclient.progress.Registration;
import org.openthinclient.web.SchemaService;
import org.openthinclient.web.pkgmngr.ui.presenter.AvailablePackageListMasterDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageActionOverviewPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsListPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.UpdateablePackageListMasterDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.view.PackageActionOverviewView;
import org.openthinclient.web.pkgmngr.ui.view.PackageListMasterDetailsView;
import org.openthinclient.web.pkgmngr.ui.view.PackageManagerMainView;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.annotation.PreDestroy;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGEMANAGERMAINNAVIGATORVIEW_CAPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGEMANAGER_TAB_AVAILABLEPACKAGES;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGEMANAGER_TAB_INSTALLEDPACKAGES;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGEMANAGER_TAB_UPDATEABLEPACKAGES;

@SpringView(name = "package-management")
@SideBarItem(sectionId = DashboardSections.PACKAGE_MANAGEMENT, captionCode = "UI_PACKAGEMANAGERMAINNAVIGATORVIEW_CAPTION")
public class PackageManagerMainNavigatorView extends Panel implements View {

  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = -1596921762830560217L;
  /**
   * LOGGER
   */
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
    mainView.addSelectedTabChangeListener(event -> {
          PackageManagerMainNavigatorView.this.updateablePackagesPresenter.refreshUpdatePanel();
          PackageManagerMainNavigatorView.this.availablePackagesPresenter.refreshUpdatePanel();
    });

    this.availablePackagesPresenter = createPresenter(PackageDetailsListPresenter.Mode.INSTALL, mainView.getAvailablePackagesView());
    this.updateablePackagesPresenter = createPresenter(PackageDetailsListPresenter.Mode.UPDATE, mainView.getUpdateablePackagesView());
    this.installedPackagesPresenter = createPresenter(PackageDetailsListPresenter.Mode.UNINSTALL, mainView.getInstalledPackagesView());

    // handle sourceUpdatePanel-view
    mainView.getInstalledPackagesView().hideSourceUpdatePanel();

    root.addComponent(mainView);
    root.setExpandRatio(mainView, 1);

    handler = packageManagerExecutionEngine.addTaskFinalizedHandler(e -> {
      bindPackageLists();
    });

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

    final PackageDetailsListPresenter packageDetailsListPresenter = new PackageDetailsListPresenter(mode, new PackageActionOverviewPresenter(packageActionOverviewView), packageManager, schemaService, applicationService);

    Consumer<Collection<Package>> presenter = packageDetailsListPresenter::setPackages;
    if (mode == PackageDetailsListPresenter.Mode.INSTALL) {
      // in case of the installation mode, we're using the AvailablePackageListMasterDetailsPresenter
      // as it does some additional filtering of the package list, specific to the handling of
      // packages that may be installed
      return new AvailablePackageListMasterDetailsPresenter(masterDetailsView, presenter, packageManager);
    }
    if (mode == PackageDetailsListPresenter.Mode.UPDATE) {
      // similar to the installation mode, update has some specific behaviour that will be handled
      // using the following Presenter
      return new UpdateablePackageListMasterDetailsPresenter(masterDetailsView, presenter, packageManager);
    }
    return new PackageListMasterDetailsPresenter(masterDetailsView, presenter, packageManager);
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
    LOGGER.debug("Cleaup {} and unregister {}", this, handler);
    handler.unregister();
  }

}

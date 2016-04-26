package org.openthinclient.web.pkgmngr.ui;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.view.PackageListMasterDetailsView;
import org.openthinclient.web.pkgmngr.ui.view.PackageManagerMainView;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import java.util.Collection;
import java.util.concurrent.Callable;

@SpringView(name = "package-management")
@SideBarItem(sectionId = DashboardSections.PACKAGE_MANAGEMENT, caption = "Manage Packages")
public class PackageManagerMainNavigatorView extends Panel implements View {

    private final VerticalLayout root;
    private final PackageListMasterDetailsPresenter availablePackagesPresenter;
    private final PackageListMasterDetailsPresenter installedPackagesPresenter;
    private final PackageManager packageManager;

    @Autowired
    public PackageManagerMainNavigatorView(final PackageManager packageManager) {
        this.packageManager = packageManager;
        addStyleName(ValoTheme.PANEL_BORDERLESS);
        setSizeFull();

        final PackageManagerMainView mainView = new PackageManagerMainView();

        this.availablePackagesPresenter = createPresenter(mainView.getAvailablePackagesView());
        this.installedPackagesPresenter = createPresenter(mainView.getInstalledPackagesView());


        root = new VerticalLayout();
        root.setSizeFull();
        root.setMargin(true);
        setContent(root);

        root.addComponent(new ViewHeader("Package Management"));

        root.addComponent(buildSparklines());

        root.addComponent(mainView);
        root.setExpandRatio(mainView, 1);
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
        return new PackageListMasterDetailsPresenter(masterDetailsView, new PackageDetailsPresenter(masterDetailsView.getPackageDetailsView(), packageManager));
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


}

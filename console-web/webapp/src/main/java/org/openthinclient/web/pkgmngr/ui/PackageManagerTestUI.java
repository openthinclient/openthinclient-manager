package org.openthinclient.web.pkgmngr.ui;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.UI;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;
import org.openthinclient.web.pkgmngr.ui.view.PackageListMasterDetailsView;
import org.openthinclient.web.pkgmngr.ui.view.PackageManagerMainView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.concurrent.Callable;

@Theme("valo")
@SpringUI(path = "/package-manager-test")
public class PackageManagerTestUI extends UI {

  @Autowired
  PackageManager packageManager;

  @Override
  protected void init(VaadinRequest request) {

    final PackageManagerMainView mainView = new PackageManagerMainView();

    final PackageListMasterDetailsPresenter availablePackagesPresenter = createPresenter(mainView.getAvailablePackagesView());
    final PackageListMasterDetailsPresenter installedPackagesPresenter = createPresenter(mainView.getInstalledPackagesView());

    bindPackageList(availablePackagesPresenter, packageManager::getInstallablePackages);
    bindPackageList(installedPackagesPresenter, packageManager::getInstalledPackages);

    setContent(mainView);

  }

  private PackageListMasterDetailsPresenter createPresenter(PackageListMasterDetailsView masterDetailsView) {
    return new PackageListMasterDetailsPresenter(masterDetailsView, new PackageDetailsPresenter(masterDetailsView.getPackageDetailsView()));
  }

  private void bindPackageList(PackageListMasterDetailsPresenter presenter, Callable<Collection<org.openthinclient.util.dpkg.Package>> packagesProvider) {
    try {
      presenter.setPackages(packagesProvider.call());
    } catch (Exception e) {

      presenter.showPackageListLoadingError(e);
      // FIXME
      e.printStackTrace();
    }

  }

}

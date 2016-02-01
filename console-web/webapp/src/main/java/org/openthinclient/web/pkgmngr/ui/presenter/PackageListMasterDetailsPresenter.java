package org.openthinclient.web.pkgmngr.ui.presenter;

import org.openthinclient.pkgmgr.db.Package;

import java.util.Collection;
import java.util.function.Consumer;

public class PackageListMasterDetailsPresenter {

  private final View view;
  private final PackageDetailsPresenter detailsPresenter;

  public PackageListMasterDetailsPresenter(View view, PackageDetailsPresenter detailsPresenter) {
    this.view = view;
    this.detailsPresenter = detailsPresenter;

    // basic wiring.
    view.onPackageSelected(detailsPresenter::setPackage);
  }

  public void showPackageListLoadingError(Exception e) {

    // FIXME implement me!

  }

  public void setPackages(Collection<Package> packages) {

    view.clearPackageList();

    packages.forEach(view::addPackage);
    detailsPresenter.setPackage(null);

  }

  public interface View {

    void clearPackageList();

    void addPackage(Package otcPackage);

    void onPackageSelected(Consumer<Package> consumer);


  }

}

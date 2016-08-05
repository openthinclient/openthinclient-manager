package org.openthinclient.web.pkgmngr.ui.view;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.pkgmngr.ui.design.PackageListMasterDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;

import java.util.function.Consumer;

public class PackageListMasterDetailsView extends PackageListMasterDetailsDesign implements PackageListMasterDetailsPresenter.View {

  /** serialVersionUID */
  private static final long serialVersionUID = 6572660094735789367L;
  
  private final PackageListContainer packageListContainer;

  public PackageListMasterDetailsView() {

    packageListContainer = new PackageListContainer();
    packageList.setContainerDataSource(packageListContainer);

    packageList.setVisibleColumns("name", "version");

  }

  @Override
  public void clearPackageList() {
    packageList.clear();
    packageListContainer.removeAllItems();
  }

  @Override
  public void addPackage(Package otcPackage) {
    packageListContainer.addItem(otcPackage);
  }

  @Override
  public void onPackageSelected(Consumer<Package> consumer) {
    packageList.addValueChangeListener(event -> {
      final Object selectedItem = packageList.getValue();
      if (selectedItem != null && selectedItem instanceof Package) {
        consumer.accept((Package) selectedItem);
      } else
      // we're treating this as a "empty" selection
      {
        consumer.accept(null);
      }
    });
  }

  public PackageDetailsView getPackageDetailsView() {
    return packageDetails;
  }
}

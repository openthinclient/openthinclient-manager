package org.openthinclient.web.pkgmngr.ui.view;

import java.util.Collection;
import java.util.function.Consumer;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.pkgmngr.ui.design.PackageListMasterDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;

public class PackageListMasterDetailsView extends PackageListMasterDetailsDesign implements PackageListMasterDetailsPresenter.View {

  /** serialVersionUID */
  private static final long serialVersionUID = 6572660094735789367L;
  
  private final PackageListContainer packageListContainer;

  public PackageListMasterDetailsView() {

    packageListContainer = new PackageListContainer();
    packageList.setContainerDataSource(packageListContainer);
    packageList.setVisibleColumns("name", "version");
    packageList.setMultiSelect(true);
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
  public void onPackageSelected(Consumer<Collection<Package>> consumer) {
    packageList.addValueChangeListener(event -> {
      Collection<Package> value = (Collection<Package>) event.getProperty().getValue();
      consumer.accept(value);
    });
  }

  public PackageDetailsListView getPackageDetailsView() {
    return packageDetailsList;
  }
}

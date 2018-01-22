package org.openthinclient.web.pkgmngr.ui.presenter;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;

import java.util.Collection;
import java.util.function.Consumer;

public class UpdateablePackageListMasterDetailsPresenter extends PackageListMasterDetailsPresenter {
  public UpdateablePackageListMasterDetailsPresenter(View view, Consumer<Collection<Package>> detailsPresenter, PackageManager packageManager) {
    super(view, detailsPresenter, packageManager);
  }

  @Override
  protected void applyFilters() {
    super.applyFilters();

    dataProvider.addFilter(new LatestVersionOnlyFilter(dataProvider));
  }
}

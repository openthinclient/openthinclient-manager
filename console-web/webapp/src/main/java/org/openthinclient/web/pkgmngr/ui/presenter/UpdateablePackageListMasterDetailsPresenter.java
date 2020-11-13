package org.openthinclient.web.pkgmngr.ui.presenter;

import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.service.nfs.NFSService;

import java.util.Collection;
import java.util.function.Consumer;

public class UpdateablePackageListMasterDetailsPresenter extends PackageListMasterDetailsPresenter {
  public UpdateablePackageListMasterDetailsPresenter(View view, Consumer<Collection<Package>> detailsPresenter, PackageManager packageManager, ClientService clientService, NFSService nfsService) {
    super(view, detailsPresenter, packageManager, clientService, nfsService);
  }

  @Override
  protected void applyFilters() {
    super.applyFilters();

    dataProvider.addFilter(new LatestVersionOnlyFilter(dataProvider));
  }
}

//package org.openthinclient.flow.packagemanager.presenter;
//
//import org.openthinclient.common.model.service.ClientService;
//import org.openthinclient.pkgmgr.PackageManager;
//import org.openthinclient.pkgmgr.db.Package;
//
//import java.util.Collection;
//import java.util.function.Consumer;
//
//public class UpdateablePackageListMasterDetailsPresenter extends PackageListMasterDetailsPresenter {
//  public UpdateablePackageListMasterDetailsPresenter(View view, Consumer<Collection<Package>> detailsPresenter, PackageManager packageManager, ClientService clientService) {
//    super(view, detailsPresenter, packageManager, clientService);
//  }
//
//  @Override
//  protected void applyFilters() {
//    super.applyFilters();
//
//    dataProvider.addFilter(new LatestVersionOnlyFilter(dataProvider));
//  }
//}

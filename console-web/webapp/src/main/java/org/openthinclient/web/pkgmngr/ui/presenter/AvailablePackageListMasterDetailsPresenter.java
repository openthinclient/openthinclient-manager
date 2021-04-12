package org.openthinclient.web.pkgmngr.ui.presenter;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.SerializablePredicate;

import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.pkgmngr.ui.view.ResolvedPackageItem;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AvailablePackageListMasterDetailsPresenter extends PackageListMasterDetailsPresenter {
  public AvailablePackageListMasterDetailsPresenter(View view, Consumer<Collection<Package>> detailsPresenter, PackageManager packageManager, ClientService clientService, ApplicationContext applicationContext) {
    super(view, detailsPresenter, packageManager, clientService, applicationContext);

    // enable the "show all versions" filtering checkbox
    view.getAllPackagesFilterCheckbox().setVisible(true);
    view.getPreviewPackagesFilterCheckbox().setVisible(true);

  }

  @Override
  protected void applyFilters() {
    super.applyFilters();
    Boolean showPreviewVersions = this.view.getPreviewPackagesFilterCheckbox().getValue();
    if (!showPreviewVersions) {
      dataProvider.addFilter(new NoPreviewFilter(dataProvider));
    }

    // handle packages-version filter
    Boolean showAllVersions = this.view.getAllPackagesFilterCheckbox().getValue();
    if (!showAllVersions) {

      // if not all theoretically installable should be shown, we're limiting the list to
      // - packages that have not yet been installed. (The identification will be done using the name)
      dataProvider.addFilter(new NotInstalledFilter(packageManager));
      // - only the latest version of a package
      dataProvider.addFilter(new LatestVersionOnlyFilter(dataProvider));
    }

  }

  /**
   * Filter out installed packages, but include the latest preview version if it is newer than the stable version of an
   * installed package.
   */
  public static class NotInstalledFilter implements SerializablePredicate<ResolvedPackageItem> {

    private Map<String, Package> installedPackages;

    public NotInstalledFilter(PackageManager packageManager) {
      installedPackages = packageManager.getInstalledPackages().stream()
                                          .collect(Collectors.toMap(Package::getName, pkg -> pkg));
    }

    @Override
    public boolean test(ResolvedPackageItem item) {
      return ( !installedPackages.containsKey(item.getName())
              || (item.getPackage().getVersion().isPreview()
                  && !installedPackages.get(item.getName()).getVersion().isPreview()
                  )
              );
    }
  }

  /**
   * Filters out the preview versions of {@link Package packages}.
   */
  public static class NoPreviewFilter implements SerializablePredicate<ResolvedPackageItem> {
    private final List<Package> previewVersionPackageList;

    public NoPreviewFilter(ListDataProvider<ResolvedPackageItem> dataProvider) {
      previewVersionPackageList = dataProvider.getItems().stream()
                                              .map(item -> item.getPackage())
                                              .filter(pkg -> pkg.getVersion().isPreview())
                                              .collect(Collectors.toList());

    }

    @Override
    public boolean test(ResolvedPackageItem item) {
      return !previewVersionPackageList.contains(item.getPackage());
    }
  }

}

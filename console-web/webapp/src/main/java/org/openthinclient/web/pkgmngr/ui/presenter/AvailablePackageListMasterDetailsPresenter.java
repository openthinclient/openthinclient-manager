package org.openthinclient.web.pkgmngr.ui.presenter;

import com.vaadin.server.SerializablePredicate;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.pkgmngr.ui.view.AbstractPackageItem;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AvailablePackageListMasterDetailsPresenter extends PackageListMasterDetailsPresenter {
  public AvailablePackageListMasterDetailsPresenter(View view, Consumer<Collection<Package>> detailsPresenter, PackageManager packageManager) {
    super(view, detailsPresenter, packageManager);

    // enable the "show all versions" filtering checkbox
    view.getPackageFilerCheckbox().setVisible(true);

  }

  @Override
  protected void applyFilters() {
    super.applyFilters();

    // handle packages-version filter
    Boolean showAllVersions = this.view.getPackageFilerCheckbox().getValue();
    if (!showAllVersions) {

      // if not all theoretically installable should be shown, we're limiting the list to
      // - packages that have not yet been installed. (The identification will be done using the name)
      dataProvider.addFilter(new NotInstalledFilter(packageManager));
      // - only the latest version of a package
      dataProvider.addFilter(new LatestVersionOnlyFilter(dataProvider));
    }

  }

  /**
   * A filter that will match all packages that do not have a name equal to a package that has already been installed.
   * This filter effectively removes all packages that have already been installed, regardless of the installed version.
   */
  public static class NotInstalledFilter implements SerializablePredicate<AbstractPackageItem> {

    private final Collection<String> installedPackageNames;

    public NotInstalledFilter(PackageManager packageManager) {
      this.installedPackageNames = packageManager.getInstalledPackages().stream().map(Package::getName).collect(Collectors.toList());
    }

    @Override
    public boolean test(AbstractPackageItem item) {
      return !installedPackageNames.contains(item.getName());
    }
  }

}

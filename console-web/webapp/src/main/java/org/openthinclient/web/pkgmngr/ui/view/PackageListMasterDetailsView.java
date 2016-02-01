package org.openthinclient.web.pkgmngr.ui.view;

import com.vaadin.data.Container;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.pkgmngr.ui.design.PackageListMasterDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;
import org.vaadin.viritin.ListContainer;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

public class PackageListMasterDetailsView extends PackageListMasterDetailsDesign implements PackageListMasterDetailsPresenter.View {

  private final PackageListContainer packageListContainer;

  public PackageListMasterDetailsView() {

    packageListContainer = new PackageListContainer();
    packageList.setContainerDataSource(packageListContainer);

    packageList.setVisibleColumns("name", "version");

  }

  @Override
  public void clearPackageList() {
    packageList.clear();
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

  public static class PackageListContainer extends ListContainer<Package> implements Container.Hierarchical {

    public PackageListContainer() {
      super(Package.class);
    }

    @Override
    public Collection<?> getChildren(Object itemId) {
      // no grouping/child support yet.
      return Collections.emptyList();
    }

    @Override
    public Object getParent(Object itemId) {
      // no grouping/child support yet.
      return null;
    }

    @Override
    public Collection<?> rootItemIds() {
      return getItemIds();
    }

    @Override
    public boolean setParent(Object itemId, Object newParentId) throws UnsupportedOperationException {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean areChildrenAllowed(Object itemId) {
      return false;
    }

    @Override
    public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) throws UnsupportedOperationException {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRoot(Object itemId) {

      return getItemIds().contains(itemId);
    }

    @Override
    public boolean hasChildren(Object itemId) {
      return false;
    }
  }
}

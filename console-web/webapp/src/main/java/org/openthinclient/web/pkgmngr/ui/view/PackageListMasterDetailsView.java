package org.openthinclient.web.pkgmngr.ui.view;

import java.util.Collection;
import java.util.function.Consumer;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.pkgmngr.ui.design.PackageListMasterDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;

import com.vaadin.data.Container.Filter;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextField;

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
  @SuppressWarnings("unchecked")
  public void onPackageSelected(Consumer<Collection<Package>> consumer) {
    packageList.addValueChangeListener(event -> {
      Collection<Package> value = (Collection<Package>) event.getProperty().getValue();
      consumer.accept(value);
    });
  }

  public PackageDetailsListView getPackageDetailsView() {
    return packageDetailsList;
  }

  @Override
  public Button getSearchButton() {
    return searchButton;
  }

  @Override
  public TextField getSearchField() {
    return searchTextField;
  }

  @Override
  public void addContainerFilter(Filter filter) {
    packageListContainer.addContainerFilter(filter);
  }

  @Override
  public void removeContainerFilter(Filter filter) {
    packageListContainer.removeContainerFilter(filter);
  }

  @Override
  public void removeAllContainerFilters() {
    packageListContainer.removeAllContainerFilters();
    // TODO: magic numbers - hässlich! aber ohne der zeile aktualisiert vaadin die ansicht ohne filter nicht richtig: die scrollbar fehlt
    packageList.setHeight(39 + (packageListContainer.size() * 38) + "px");
  }

}

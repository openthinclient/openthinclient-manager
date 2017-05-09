package org.openthinclient.web.pkgmngr.ui.view;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.ui.*;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.pkgmngr.ui.design.PackageListMasterDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PackageListMasterDetailsView extends PackageListMasterDetailsDesign implements PackageListMasterDetailsPresenter.View {

  /** serialVersionUID */
  private static final long serialVersionUID = 6572660094735789367L;
  
//  private final PackageListContainer packageListContainer;
  private DataProvider<AbstractPackageItem, ?> packageListDataProvider;

  public PackageListMasterDetailsView() {

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    packageListDataProvider =  DataProvider.ofCollection(Collections.EMPTY_LIST);
    packageList.setDataProvider(packageListDataProvider);
    packageList.setSelectionMode(Grid.SelectionMode.MULTI);
    packageList.addColumn(AbstractPackageItem::getName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    packageList.addColumn(AbstractPackageItem::getDisplayVersion).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));

  }

  @Override
  public Collection<AbstractPackageItem> getItems() {
//     return packageListContainer.getItemIds();
    return Collections.EMPTY_LIST;
  }
  
  @Override
  public void clearPackageList() {
//    packageList.clear();
//    packageListContainer.removeAllItems();
  }

  @Override
  public void setPackages(List<AbstractPackageItem> otcPackages) {
    packageListDataProvider = DataProvider.ofCollection(otcPackages);
    packageList.setDataProvider(packageListDataProvider);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onPackageSelected(Consumer<Collection<Package>> consumer) {
//    packageList.addValueChangeListener(event -> {
//      Collection<ResolvedPackageItem> value = (Collection<ResolvedPackageItem>) event.getProperty().getValue();
//      consumer.accept(value.stream().map(rpi -> rpi.getPackage()).collect(Collectors.toCollection(ArrayList::new)));
//    });
    packageList.addSelectionListener(event -> {
      Set<AbstractPackageItem> value = event.getAllSelectedItems();
      consumer.accept(value.stream().map(rpi -> ((ResolvedPackageItem) rpi).getPackage()).collect(Collectors.toCollection(ArrayList::new)));
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

//  @Override
//  public void addContainerFilter(Filter filter) {
//    packageListContainer.addContainerFilter(filter);
//    packageList.refreshRowCache();
//    adjustHeight();
//  }

//  @Override
//  public void removeContainerFilter(Filter filter) {
//    packageListContainer.removeContainerFilter(filter);
//    packageList.refreshRowCache();
//    adjustHeight();
//  }

  @Override
  public void removeAllContainerFilters() {
//    packageListContainer.removeAllContainerFilters();
//    packageList.refreshRowCache();
//    adjustHeight();
  }

  @Override
  public CheckBox getPackageFilerCheckbox() {
    return packageFilerCheckbox;
  }

  @Override
  public void adjustHeight() {
//     packageList.setPageLength(packageList.getItemIds().size() + 1);
  }

  @Override
  public void sortPackageList(String[] propertyId, boolean[] asc) {
//     packageList.sort(propertyId, asc);
  }

}

package org.openthinclient.web.pkgmngr.ui.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.pkgmngr.ui.design.PackageListMasterDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;

import com.vaadin.data.Container.Filter;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public class PackageListMasterDetailsView extends PackageListMasterDetailsDesign implements PackageListMasterDetailsPresenter.View {

  /** serialVersionUID */
  private static final long serialVersionUID = 6572660094735789367L;
  
  private final PackageListContainer packageListContainer;

  public PackageListMasterDetailsView() {
    packageListContainer = new PackageListContainer();
    packageList.setContainerDataSource(packageListContainer);
    packageList.setVisibleColumns("name", "displayVersion");
    packageList.setMultiSelect(true);
    
    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
    packageList.setColumnHeader("name", mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
    packageList.setColumnHeader("displayVersion", mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
  }

  @Override
  public Collection<AbstractPackageItem> getItems() {
     return packageListContainer.getItemIds();
  }
  
  @Override
  public void clearPackageList() {
    packageList.clear();
    packageListContainer.removeAllItems();
  }

  @Override
  public void addPackage(AbstractPackageItem otcPackage) {
    packageListContainer.addItem(otcPackage);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onPackageSelected(Consumer<Collection<Package>> consumer) {
    packageList.addValueChangeListener(event -> {
      Collection<ResolvedPackageItem> value = (Collection<ResolvedPackageItem>) event.getProperty().getValue();
      consumer.accept(value.stream().map(rpi -> rpi.getPackage()).collect(Collectors.toCollection(ArrayList::new)));
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
    packageList.refreshRowCache();
    adjustHeight();
  }

  @Override
  public void removeContainerFilter(Filter filter) {
    packageListContainer.removeContainerFilter(filter);
    packageList.refreshRowCache();
    adjustHeight();
  }

  @Override
  public void removeAllContainerFilters() {
    packageListContainer.removeAllContainerFilters();
    packageList.refreshRowCache();
    adjustHeight();
  }

  @Override
  public CheckBox getPackageFilerCheckbox() {
    return packageFilerCheckbox;
  }

  @Override
  public void adjustHeight() {
     packageList.setPageLength(packageList.getItemIds().size() + 1);
  }

}

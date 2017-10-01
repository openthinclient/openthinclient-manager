package org.openthinclient.web.pkgmngr.ui.view;

import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.ComponentRenderer;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.pkgmngr.ui.design.PackageListMasterDetailsDesign;
import org.openthinclient.web.pkgmngr.ui.presenter.PackageListMasterDetailsPresenter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public class PackageListMasterDetailsView extends PackageListMasterDetailsDesign implements PackageListMasterDetailsPresenter.View {

  /** serialVersionUID */
  private static final long serialVersionUID = 6572660094735789367L;
  
  private DataProvider<AbstractPackageItem, ?> packageListDataProvider;
  private Consumer<Package> showPackageDetailsConsumer;

  public PackageListMasterDetailsView() {

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    packageListDataProvider =  DataProvider.ofCollection(Collections.EMPTY_LIST);
    packageList.setDataProvider(packageListDataProvider);
    packageList.setSelectionMode(Grid.SelectionMode.MULTI);
    packageList.addColumn(AbstractPackageItem::getName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME)).setId("name");
    packageList.addColumn(AbstractPackageItem::getDisplayVersion).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION)).setId("displayVersion");

    packageList.addColumn((ValueProvider<AbstractPackageItem, Component>) item -> {
      final Button moreButton = new Button();
      moreButton.setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_LIST_DETAILS_CAPTION));
      moreButton.setIcon(VaadinIcons.INFO_CIRCLE_O);
      moreButton.addStyleNames(ValoTheme.BUTTON_LINK);
      moreButton.addClickListener(e -> {
        if (showPackageDetailsConsumer != null && item instanceof ResolvedPackageItem)
          showPackageDetailsConsumer.accept(((ResolvedPackageItem)item).getPackage());
      });
      return moreButton;
    }, new ComponentRenderer()).setCaption("");

  }

  @Override
  public Collection<AbstractPackageItem> getItems() {
    return Collections.EMPTY_LIST;
  }
  
  @Override
  public void setPackages(List<AbstractPackageItem> otcPackages) {
    packageListDataProvider = DataProvider.ofCollection(otcPackages);
    packageList.setDataProvider(packageListDataProvider);
  }

  @Override
  public Grid<AbstractPackageItem> getPackageList() {
    return packageList;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onPackageSelected(Consumer<Collection<Package>> consumer) {
    packageList.addSelectionListener(event -> {
      Set<AbstractPackageItem> value = event.getAllSelectedItems();
      consumer.accept(value.stream().map(rpi -> ((ResolvedPackageItem) rpi).getPackage()).collect(Collectors.toCollection(ArrayList::new)));
    });
  }

  @Override
  public void onShowPackageDetails(Consumer<Package> consumer) {
    showPackageDetailsConsumer = consumer;
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
  public CheckBox getPackageFilerCheckbox() {
    return packageFilerCheckbox;
  }


}

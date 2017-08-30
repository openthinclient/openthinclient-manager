package org.openthinclient.web.pkgmngr.ui.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.*;
import org.openthinclient.pkgmgr.PackageManagerUtils;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.pkgmngr.ui.view.AbstractPackageItem;
import org.openthinclient.web.pkgmngr.ui.view.ResolvedPackageItem;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PackageListMasterDetailsPresenter {

  private final View view;
  private final PackageDetailsListPresenter detailsPresenter;
  
  public PackageListMasterDetailsPresenter(View view, PackageDetailsListPresenter detailsPresenter) {
    this.view = view;
    this.detailsPresenter = detailsPresenter;

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
    
    // basic wiring.
    view.onPackageSelected(detailsPresenter::setPackages);
    
    // filter checkBox
    this.view.getPackageFilerCheckbox().setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_SHOW_ALL_VERSIONS));
    this.view.getPackageFilerCheckbox().setValue(false);
    this.view.getPackageFilerCheckbox().addValueChangeListener(e -> {
        applyFilters();
    });
    
    // search
    this.view.getSearchButton().addClickListener(e -> {
        applyFilters();
    });
    
    this.view.getSearchField().setPlaceholder(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_SEARCHFIELD_INPUTPROMT));
    this.view.getSearchField().addValueChangeListener(e -> {
        applyFilters();
    });


  }

  /**
   * Specifies whether or not the "Show All Versions" button is shown.
   */
  public void setVersionFilteringAllowed(boolean value) {
    view.getPackageFilerCheckbox().setVisible(value);
  }

  private void applyFilters() {
      ListDataProvider<AbstractPackageItem> dataProvider = (ListDataProvider<AbstractPackageItem>) view.getPackageList().getDataProvider();

      PackageManagerUtils pmu = new PackageManagerUtils();
      List<Package> latestVersionPackageList = pmu.reduceToLatestVersion(dataProvider.getItems().stream().filter(api -> (api instanceof ResolvedPackageItem)).map(api -> ((ResolvedPackageItem) api).getPackage()).collect(Collectors.toList()));

      dataProvider.clearFilters();

      // handle packages-version filter
      Boolean isChecked = this.view.getPackageFilerCheckbox().getValue();
      if (!isChecked) {
          dataProvider.addFilter(abstractPackageItem -> latestVersionPackageList.stream().filter(p -> p.compareTo(((ResolvedPackageItem) abstractPackageItem).getPackage()) == 0).findAny().isPresent());
      }

      // handle package-name filter
      String value = view.getSearchField().getValue().trim();
      if (!value.isEmpty()) {
          dataProvider.addFilter(AbstractPackageItem::getName, s -> s.toLowerCase().startsWith(value.toLowerCase()));
      }

  }

  public void showPackageListLoadingError(Exception e) {
    // FIXME implement me!
  }

  public void setPackages(Collection<Package> packages) {

    view.setPackages(packages.stream().map(ResolvedPackageItem::new).collect(Collectors.toList()));

    // set new filter if checkbox is checked
    applyFilters();
    view.getPackageList().sort("name", SortDirection.ASCENDING);
  }

  public interface View {

    Button getSearchButton();

    TextField getSearchField();

    void onPackageSelected(Consumer<Collection<Package>> consumer);

    CheckBox getPackageFilerCheckbox();

    Collection<AbstractPackageItem> getItems();

    void setPackages(List<AbstractPackageItem> collect);

    Grid<AbstractPackageItem> getPackageList();
  }

}

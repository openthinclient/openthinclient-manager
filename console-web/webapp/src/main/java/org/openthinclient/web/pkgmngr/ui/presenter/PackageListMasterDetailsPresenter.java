package org.openthinclient.web.pkgmngr.ui.presenter;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerUtils;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.pkgmngr.ui.view.AbstractPackageItem;
import org.openthinclient.web.pkgmngr.ui.view.PackageDetailsView;
import org.openthinclient.web.pkgmngr.ui.view.PackageDetailsWindow;
import org.openthinclient.web.pkgmngr.ui.view.ResolvedPackageItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public class PackageListMasterDetailsPresenter {

  private final View view;
  private final ListDataProvider<AbstractPackageItem> dataProvider;

  public PackageListMasterDetailsPresenter(View view, PackageDetailsListPresenter detailsPresenter, PackageManager packageManager) {
    this.view = view;
    this.dataProvider = new ListDataProvider<>(new ArrayList<>());
    this.view.setDataProvider(this.dataProvider);

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

    this.view.onShowPackageDetails((pkg) -> {
      final PackageDetailsView packageDetailsView = new PackageDetailsView();
      final PackageDetailsPresenter presenter = new PackageDetailsPresenter(new PackageDetailsWindow(packageDetailsView, packageDetailsView), packageManager);
      // setting the package will automatically trigger the view to be shown
      presenter.setPackage(pkg);
    });

  }

  /**
   * Specifies whether or not the "Show All Versions" button is shown.
   */
  public void setVersionFilteringAllowed(boolean value) {
    view.getPackageFilerCheckbox().setVisible(value);
  }

  private void applyFilters() {
    PackageManagerUtils pmu = new PackageManagerUtils();
    List<Package> latestVersionPackageList = pmu.reduceToLatestVersion(dataProvider.getItems().stream().filter(api -> (api instanceof ResolvedPackageItem)).map(api -> ((ResolvedPackageItem) api).getPackage()).collect(Collectors.toList()));

    dataProvider.clearFilters();

    // handle packages-version filter
    Boolean isChecked = this.view.getPackageFilerCheckbox().getValue();
    if (!isChecked) {
      dataProvider.addFilter(abstractPackageItem -> latestVersionPackageList.stream().anyMatch(p -> p.compareTo(((ResolvedPackageItem) abstractPackageItem).getPackage()) == 0));
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

    dataProvider.getItems().addAll(packages.stream().map(ResolvedPackageItem::new).collect(Collectors.toList()));

    // set new filter if checkbox is checked
    applyFilters();
    dataProvider.refreshAll();

    view.sort(View.SortableProperty.NAME, SortDirection.ASCENDING);
  }

  public interface View {

    Button getSearchButton();

    TextField getSearchField();

    void onPackageSelected(Consumer<Collection<Package>> consumer);

    void onShowPackageDetails(Consumer<Package> consumer);

    CheckBox getPackageFilerCheckbox();

    void setDataProvider(DataProvider<AbstractPackageItem, ?> dataProvider);

    void sort(SortableProperty property, SortDirection direction);

    enum SortableProperty {
      NAME("name");
      private final String beanPropertyName;

      SortableProperty(String beanPropertyName) {

        this.beanPropertyName = beanPropertyName;
      }

      public String getBeanPropertyName() {
        return beanPropertyName;
      }
    }
  }

}

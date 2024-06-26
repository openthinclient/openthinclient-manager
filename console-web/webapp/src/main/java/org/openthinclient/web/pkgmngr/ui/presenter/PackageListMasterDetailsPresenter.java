package org.openthinclient.web.pkgmngr.ui.presenter;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

import org.apache.commons.lang3.StringUtils;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerUtils;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.web.pkgmngr.ui.view.AbstractPackageItem;
import org.openthinclient.web.pkgmngr.ui.view.PackageDetailsView;
import org.openthinclient.web.pkgmngr.ui.view.PackageDetailsWindow;
import org.openthinclient.web.pkgmngr.ui.view.ResolvedPackageItem;
import org.openthinclient.web.progress.ProgressReceiverDialog;
import org.springframework.context.ApplicationContext;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SHORT_DATE_FORMAT;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGEMANAGER_LASTUPDATE_LABEL;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_PACKAGESOURCES_PROGRESS_CAPTION;

public class PackageListMasterDetailsPresenter {

  protected final View view;
  protected final ListDataProvider<ResolvedPackageItem> dataProvider;
  protected final PackageManager packageManager;
  protected final IMessageConveyor mc;
  protected final boolean applicationIsPreview;

  public PackageListMasterDetailsPresenter(View view, Consumer<Collection<Package>> detailsPresenter, PackageManager packageManager, ClientService clientService, ApplicationContext applicationContext) {
    this.view = view;
    this.dataProvider = new ListDataProvider<>(new ArrayList<>());
    dataProvider.setSortOrder(ResolvedPackageItem::getName,
                              SortDirection.ASCENDING);
    dataProvider.addSortOrder(item -> item.getPackage().getVersion(),
                              SortDirection.DESCENDING);
    this.view.setDataProvider(this.dataProvider);
    this.packageManager = packageManager;
    this.mc = new MessageConveyor(UI.getCurrent().getLocale());
    this.applicationIsPreview = applicationContext.getEnvironment().getProperty("application.is-preview", Boolean.class);

    // basic wiring.
    view.onPackageSelected(packages -> {

      if (packages == null || packages.size() == 0)
        view.setDetailsVisible(false);
      else
        view.setDetailsVisible(true);

      detailsPresenter.accept(packages);
    });

    // filter checkBox
    view.getAllPackagesFilterCheckbox().setValue(false);
    view.getAllPackagesFilterCheckbox().addValueChangeListener(e -> {
      applyFilters();
    });
    view.getPreviewPackagesFilterCheckbox().setValue(applicationIsPreview);
    view.getPreviewPackagesFilterCheckbox().addValueChangeListener(e -> {
      applyFilters();
    });
    // the filter checkboxes are only enabled in the AvailablePackageListMasterDetailsPresenter
    view.getAllPackagesFilterCheckbox().setVisible(false);
    view.getPreviewPackagesFilterCheckbox().setVisible(false);

    view.getFilterInput().addValueChangeListener(e -> {
      applyFilters();
    });

    view.onShowPackageDetails((pkg) -> {
      final PackageDetailsView packageDetailsView = new PackageDetailsView();
      final PackageDetailsPresenter
      presenter = new PackageDetailsPresenter(
                    new PackageDetailsWindow(packageDetailsView, packageDetailsView),
                    packageManager,
                    clientService,
                    applicationContext);
      // setting the package will automatically trigger the view to be shown
      presenter.setPackage(pkg);
    });

    // update sources
    view.getSourceUpdateButton().addClickListener(event -> {
      final ListenableProgressFuture<PackageListUpdateReport> update = packageManager.updateCacheDB();
      final ProgressReceiverDialog dialog = new ProgressReceiverDialog(mc.getMessage(UI_PACKAGESOURCES_PROGRESS_CAPTION)){
        @Override
        public void close() {
          super.close();
          refreshUpdatePanel();
        }
      };
      dialog.watch(update);
      dialog.open(true);
    });

  }

  /**
   * Refreshes the updatePanel view with current data
   */
  public void refreshUpdatePanel() {
    packageManager.getSourcesList().getSources().stream()
        .min(Comparator.comparing(Source::getLastUpdated, Comparator.nullsLast(Comparator.reverseOrder())))
        .ifPresent(lastUpdatedSource -> {
          if (lastUpdatedSource.getLastUpdated() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mc.getMessage(UI_SHORT_DATE_FORMAT));
            view.setSourceUpdateLabelValue(mc.getMessage(UI_PACKAGEMANAGER_LASTUPDATE_LABEL, lastUpdatedSource.getLastUpdated().format(formatter)));
          }
        });
  }

  protected void applyFilters() {

    dataProvider.clearFilters();

    // If the user entered a text in the search field, apply a filter matching only packages with a
    // name starting with the provided text
    String value = view.getFilterInput().getValue().trim();
    if (!value.isEmpty()) {
      dataProvider.addFilter(AbstractPackageItem::getName,
                              s -> s.toLowerCase().contains(value.toLowerCase()));
    }

    dataProvider.addFilter(new HideOTCManagerVersionFilter());
  }

  public void showPackageListLoadingError(Exception e) {
    // FIXME implement me!
  }

  public void setPackages(Collection<Package> packages) {

    dataProvider.getItems().clear();
    dataProvider.getItems().addAll(packages.stream().map(ResolvedPackageItem::new).collect(Collectors.toList()));

    // set new filter if checkbox is checked
    applyFilters();
    dataProvider.refreshAll();

    view.clearSelection();

    view.sort(View.SortableProperty.NAME, SortDirection.ASCENDING);
  }

  public interface View {

    TextField getFilterInput();

    void onPackageSelected(Consumer<Collection<Package>> consumer);

    void onShowPackageDetails(Consumer<Package> consumer);

    CheckBox getAllPackagesFilterCheckbox();

    CheckBox getPreviewPackagesFilterCheckbox();

    void setDataProvider(DataProvider<ResolvedPackageItem, ?> dataProvider);

    void sort(SortableProperty property, SortDirection direction);

    void setDetailsVisible(boolean visible);

    void hideSourceUpdatePanel();

    void setSourceUpdateLabelValue(String text);

    Button getSourceUpdateButton();

    void clearSelection();

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

  /**
   * Filters all but the latest versions of {@link Package packages}.
   */
  public static class LatestVersionOnlyFilter implements SerializablePredicate<ResolvedPackageItem> {
    private final List<Package> latestVersionPackageList;

    public LatestVersionOnlyFilter(ListDataProvider<ResolvedPackageItem> dataProvider) {
      PackageManagerUtils pmu = new PackageManagerUtils();
      latestVersionPackageList = pmu.reduceToLatestVersion(
                                      dataProvider.getItems().stream()
                                                  .map(item -> item.getPackage())
                                                  .collect(Collectors.toList())
                                  );

    }

    @Override
    public boolean test(ResolvedPackageItem item) {
      final Package pkg = ((ResolvedPackageItem) item).getPackage();
      return latestVersionPackageList.stream().anyMatch(p -> {
        return p.compareTo(pkg) == 0
                // in some cases, a package with identical name and version might appear in multiple
                // repositories. To prevent that, we're filtering based on the appropriate Source, too.
                && p.getSource().equals(pkg.getSource());
      });
    }
  }

  /**
   * A filter to hide OPENTHINCLIENT_MANAGER_VERSION_NAME package
   */
  public static class HideOTCManagerVersionFilter implements SerializablePredicate<ResolvedPackageItem> {

    public static final String OPENTHINCLIENT_MANAGER_VERSION_NAME="openthinclient-manager-version";

    @Override
    public boolean test(ResolvedPackageItem item) {
      return !StringUtils.equalsIgnoreCase(item.getName(), OPENTHINCLIENT_MANAGER_VERSION_NAME);
    }
  }
}

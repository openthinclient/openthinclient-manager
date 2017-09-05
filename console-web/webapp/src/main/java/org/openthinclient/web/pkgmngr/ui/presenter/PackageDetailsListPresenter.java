package org.openthinclient.web.pkgmngr.ui.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.*;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.web.SchemaService;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.pkgmngr.ui.AffectedApplicationsSummaryDialog;
import org.openthinclient.web.pkgmngr.ui.InstallationPlanSummaryDialog;
import org.openthinclient.web.pkgmngr.ui.view.AbstractPackageItem;
import org.openthinclient.web.pkgmngr.ui.view.PackageDetailsView;
import org.openthinclient.web.pkgmngr.ui.view.ResolvedPackageItem;
import org.openthinclient.web.progress.ProgressReceiverDialog;
import org.vaadin.viritin.button.MButton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Stream.concat;
import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

public class PackageDetailsListPresenter {

  private final View view;
  private final PackageManager packageManager;
  private final SchemaService schemaService;
  private final ApplicationService applicationService;
  private final IMessageConveyor mc;

  public PackageDetailsListPresenter(View view, PackageManager packageManager, SchemaService schemaService, ApplicationService applicationService) {
    this.view = view;
    this.packageManager = packageManager;
    this.schemaService = schemaService;
    this.applicationService = applicationService;
    mc = new MessageConveyor(UI.getCurrent().getLocale());
  }

  public void setPackages(Collection<org.openthinclient.pkgmgr.db.Package> otcPackages) {

    if (otcPackages != null) {
      view.show();
      view.clearPackageList();
      view.getActionBar().removeAllComponents();

      List<Component> installable = new ArrayList<>();
      List<Component> uninstallable = new ArrayList<>();

      for (Package otcPackage : otcPackages) {

        PackageDetailsView detailsView = new PackageDetailsView();

        detailsView.setName(otcPackage.getName());
        detailsView.setVersion(otcPackage.getVersion().toString());
        detailsView.setDescription(otcPackage.getDescription());
        detailsView.setShortDescription(otcPackage.getShortDescription());
        detailsView.setSourceUrl(otcPackage.getSource().getUrl().toString());
        detailsView.setChangeLog(otcPackage.getChangeLog());

        List<Package> installableAndExistingPackages = concat(
                packageManager.getInstalledPackages().stream(),
                packageManager.getInstallablePackages().stream()
        ).sorted()
         .collect(Collectors.toList());

        List<String> usedPackages = new ArrayList<>();
        // depends
        detailsView.addDependencies(PackageDetailsUtil.getReferencedPackageItems(otcPackage.getDepends(), installableAndExistingPackages, usedPackages));

        // conflicts
        if (otcPackage.getConflicts().isEmpty()) {
          detailsView.hideConflictsTable();
        } else {
          detailsView.addConflicts(PackageDetailsUtil.getReferencedPackageItems(otcPackage.getConflicts(), installableAndExistingPackages, usedPackages));
        }

        // provides
        if (otcPackage.getProvides().isEmpty()) {
          detailsView.hideProvidesTable();
        } else {
          detailsView.addProvides(PackageDetailsUtil.getReferencedPackageItems(otcPackage.getProvides(), installableAndExistingPackages, usedPackages));
        }

        view.addPackageDetails(detailsView);

        // summary headline
        if (packageManager.isInstallable(otcPackage)) {
          installable.add(createLabel(otcPackage));
        }
        if (packageManager.isInstalled(otcPackage)) {
          uninstallable.add(createLabel(otcPackage));
        }

      }

      //  attach summary header and action button
      if (!installable.isEmpty()) {
        VerticalLayout vl = new VerticalLayout();
        vl.setMargin(false);
        vl.setSpacing(true);
        vl.addComponent(new Label(installable.size() == 1 ? mc.getMessage(UI_PACKAGEMANAGER_BUTTON_INSTALL_LABEL_SINGLE) : mc.getMessage(UI_PACKAGEMANAGER_BUTTON_INSTALL_LABEL_MULTI)));
        vl.addComponent(new MButton(mc.getMessage(UI_PACKAGEMANAGER_BUTTON_INSTALL_CAPTION)).withIcon(VaadinIcons.DOWNLOAD).withListener((Button.ClickListener) event -> doInstallPackage(otcPackages)));

        // the installable list
        Grid<ResolvedPackageItem> packagesTable = new Grid();
//        // TODO: magic numbers
        packagesTable.setHeight(39 + (otcPackages.size() * 38) + "px");
        DataProvider packageListDataProvider =  DataProvider.ofCollection(otcPackages.stream().map(p -> new ResolvedPackageItem(p)).collect(Collectors.toCollection(ArrayList::new)));
        packagesTable.setDataProvider(packageListDataProvider);
        packagesTable.setSelectionMode(Grid.SelectionMode.NONE);
        packagesTable.addColumn(AbstractPackageItem::getName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
        packagesTable.addColumn(AbstractPackageItem::getDisplayVersion).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
        vl.addComponent(packagesTable);

        view.getActionBar().addComponent(vl);
      }

      if (!uninstallable.isEmpty()) {
        VerticalLayout vl = new VerticalLayout();
        vl.setMargin(false);
        vl.setSpacing(true);
        vl.addComponent(new Label(uninstallable.size() == 1 ? mc.getMessage(UI_PACKAGEMANAGER_BUTTON_UNINSTALL_LABEL_SINGLE) : mc.getMessage(UI_PACKAGEMANAGER_BUTTON_UNINSTALL_LABEL_MULTI)));
        vl.addComponent(new MButton(mc.getMessage(UI_PACKAGEMANAGER_BUTTON_UNINSTALL_CAPTION)).withIcon(VaadinIcons.TRASH).withListener((Button.ClickListener) event -> doUninstallPackage(otcPackages)));

        // the uninstallable list
        Grid<ResolvedPackageItem> packagesTable = new Grid();
        // TODO: magic numbers
        packagesTable.setHeight(39 + (otcPackages.size() * 38) + "px");
        DataProvider packageListDataProvider =  DataProvider.ofCollection(otcPackages.stream().map(p -> new ResolvedPackageItem(p)).collect(Collectors.toCollection(ArrayList::new)));
        packagesTable.setDataProvider(packageListDataProvider);
        packagesTable.setSelectionMode(Grid.SelectionMode.NONE);
        packagesTable.addColumn(AbstractPackageItem::getName).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_NAME));
        packagesTable.addColumn(AbstractPackageItem::getDisplayVersion).setCaption(mc.getMessage(ConsoleWebMessages.UI_PACKAGEMANAGER_PACKAGE_VERSION));
        vl.addComponent(packagesTable);

        view.getActionBar().addComponent(vl);
      }

      view.setHeight(600, Sizeable.Unit.PIXELS);

    } else {
      view.hide();
    }

  }

  private HorizontalLayout createLabel(Package otcPackage) {
    Label name = new Label(otcPackage.getName());
    name.setStyleName("huge");
    Label version = new Label(otcPackage.getVersion().toString());
    version.setStyleName("tiny");
    return new HorizontalLayout(name, version);
  }

  private void doInstallPackage(Collection<org.openthinclient.pkgmgr.db.Package> otcPackages) {
    final PackageManagerOperation op = packageManager.createOperation();
    otcPackages.forEach(op::install);
    op.resolve();

    // FIXME validate the state (Conflicts, missing packages, etc.)
    showSummaryDialog(op, () -> execute(op, true));

  }

  private void doUninstallPackage(Collection<org.openthinclient.pkgmgr.db.Package> otcPackages) {
    final PackageManagerOperation op = packageManager.createOperation();
    otcPackages.forEach(op::uninstall);
    op.resolve();

    final Collection<Application> affectedApplications = schemaService.findAffectedApplications(op.getInstallPlan());
    if (!affectedApplications.isEmpty()) {
      final AffectedApplicationsSummaryDialog dialog = new AffectedApplicationsSummaryDialog(affectedApplications);

      dialog.onProceed(() -> {
        dialog.close();
        showSummaryDialog(op, () -> {
          // FIXME this is the wrong place to execute this kind of logic.
          for (Application affectedApplication : affectedApplications) {
            applicationService.delete(affectedApplication);
          }

          execute(op, false);
        });
      });

      dialog.open(true);
    } else {
      // simple case without the need to
      showSummaryDialog(op, () -> execute(op, false));
    }


  }

  private void showSummaryDialog(PackageManagerOperation op, Runnable installCallback) {
    final InstallationPlanSummaryDialog summaryDialog = new InstallationPlanSummaryDialog(op, packageManager);
    summaryDialog.onInstallClicked(installCallback);
    summaryDialog.open(true);
  }

  private void execute(PackageManagerOperation op, boolean install) {
    final ProgressReceiverDialog dialog = new ProgressReceiverDialog(install ? "Installation..." : "Uninstallation...");
    final ListenableProgressFuture<PackageManagerOperationReport> future = packageManager.execute(op);
    dialog.watch(future);

    dialog.open(true);
  }

  public interface View {

    void addPackageDetails(PackageDetailsView packageDetailsView);

    void hide();

    void show();

    void clearPackageList();

    ComponentContainer getActionBar();

    void setHeight(float height, Sizeable.Unit pixels);
  }
}

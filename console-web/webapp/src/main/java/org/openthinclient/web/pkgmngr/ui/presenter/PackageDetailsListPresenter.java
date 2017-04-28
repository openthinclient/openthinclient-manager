package org.openthinclient.web.pkgmngr.ui.presenter;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.*;
import com.vaadin.v7.ui.TreeTable;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.util.dpkg.PackageReference;
import org.openthinclient.util.dpkg.PackageReference.SingleReference;
import org.openthinclient.web.SchemaService;
import org.openthinclient.web.pkgmngr.ui.AffectedApplicationsSummaryDialog;
import org.openthinclient.web.pkgmngr.ui.InstallationPlanSummaryDialog;
import org.openthinclient.web.pkgmngr.ui.view.PackageDetailsView;
import org.openthinclient.web.pkgmngr.ui.view.PackageListContainer;
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

        detailsView.clearLists();
        // Check available and existing packages to match package-reference of current package, sorted to use first matching package
        List<Package> installableAndExistingPackages = concat(
                packageManager.getInstalledPackages().stream(),
                packageManager.getInstallablePackages().stream()
        ).sorted()
                .collect(Collectors.toList());

        List<String> usedPackages = new ArrayList<>();
        for (PackageReference pr : otcPackage.getDepends()) {
          boolean isReferenced = false;
          for (Package _package : installableAndExistingPackages) {
            if (pr.matches(_package) && !usedPackages.contains(_package.getName())) {
              detailsView.addDependency(new ResolvedPackageItem(_package));
              isReferenced = true;
              usedPackages.add(_package.getName());
            }
          }
          if (!isReferenced) {
            if (pr instanceof SingleReference) {
              detailsView.addDependency(PackageDetailsUtil.createMissingPackageItem((SingleReference) pr));
            }
          }
        }
        // --

        // conflicts
        if (otcPackage.getConflicts().isEmpty()) {
          detailsView.hideConflictsTable();
        } else {
          for (PackageReference pr : otcPackage.getConflicts()) {
            boolean isReferenced = false;
            for (Package _package : installableAndExistingPackages) {
              if (pr.matches(_package) && !usedPackages.contains(_package.getName())) {
                detailsView.addConflict(new ResolvedPackageItem(_package));
                isReferenced = true;
                usedPackages.add(_package.getName());
              }
            }
            if (!isReferenced) {
              if (pr instanceof SingleReference) {
                detailsView.addConflict(PackageDetailsUtil.createMissingPackageItem("", (SingleReference) pr));
              }
            }
          }
        }

        // provides
        if (otcPackage.getProvides().isEmpty()) {
          detailsView.hideProvidesTable();
        } else {
          for (PackageReference pr : otcPackage.getProvides()) {
            boolean isReferenced = false;
            for (Package _package : installableAndExistingPackages) {
              if (pr.matches(_package) && !usedPackages.contains(_package.getName())) {
                detailsView.addProvides(new ResolvedPackageItem(_package));
                isReferenced = true;
                usedPackages.add(_package.getName());
              }
            }
            if (!isReferenced) {
              if (pr instanceof SingleReference) {
                detailsView.addProvides(PackageDetailsUtil.createMissingPackageItem("", (SingleReference) pr));
              }
            }
          }
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
        HorizontalLayout bar = new HorizontalLayout();
        bar.setSpacing(true);

        VerticalLayout vl = new VerticalLayout();
        vl.addComponent(new Label(installable.size() == 1 ? mc.getMessage(UI_PACKAGEMANAGER_BUTTON_INSTALL_LABEL_SINGLE) : mc.getMessage(UI_PACKAGEMANAGER_BUTTON_INSTALL_LABEL_MULTI)));
        vl.addComponent(new MButton(mc.getMessage(UI_PACKAGEMANAGER_BUTTON_INSTALL_CAPTION)).withIcon(FontAwesome.DOWNLOAD).withListener((Button.ClickListener) event -> doInstallPackage(otcPackages)));
        bar.addComponent(vl);

        // the installable list
        PackageListContainer packageListContainer = new PackageListContainer();
        TreeTable packagesTable = new TreeTable();
        packageListContainer.addAll(otcPackages.stream().map(p -> new ResolvedPackageItem(p)).collect(Collectors.toCollection(ArrayList::new)));
        // TODO: magic numbers
        packagesTable.setWidth("100%");
        packagesTable.setHeight(39 + (otcPackages.size() * 38) + "px");
        packagesTable.setContainerDataSource(packageListContainer);
        packagesTable.setVisibleColumns("name", "displayVersion");
        packagesTable.setColumnHeader("name", mc.getMessage(UI_PACKAGEMANAGER_PACKAGE_NAME));
        packagesTable.setColumnHeader("displayVersion", mc.getMessage(UI_PACKAGEMANAGER_PACKAGE_VERSION));
        bar.addComponent(packagesTable);
        bar.setExpandRatio(packagesTable, 3.0f); // TreeTable should use as much space as it can - but doesn't

        view.getActionBar().addComponent(bar);
      }

      if (!uninstallable.isEmpty()) {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setSpacing(true);

        VerticalLayout vl = new VerticalLayout();
        vl.addComponent(new Label(uninstallable.size() == 1 ? mc.getMessage(UI_PACKAGEMANAGER_BUTTON_UNINSTALL_LABEL_SINGLE) : mc.getMessage(UI_PACKAGEMANAGER_BUTTON_UNINSTALL_LABEL_MULTI)));
        // FIXME!! BUTTON WITHOUT FUNCTION?
        vl.addComponent(new MButton(mc.getMessage(UI_PACKAGEMANAGER_BUTTON_UNINSTALL_CAPTION)).withIcon(FontAwesome.TRASH_O));
        bar.addComponent(vl);

        // the uninstallable list
        PackageListContainer packageListContainer = new PackageListContainer();
        TreeTable packagesTable = new TreeTable();
        packageListContainer.addAll(otcPackages.stream().map(p -> new ResolvedPackageItem(p)).collect(Collectors.toCollection(ArrayList::new)));
        // TODO: magic numbers
        packagesTable.setWidth("100%");
        packagesTable.setHeight(39 + (otcPackages.size() * 38) + "px");
        packagesTable.setContainerDataSource(packageListContainer);
        packagesTable.setVisibleColumns("name", "displayVersion");
        packagesTable.setColumnHeader("name", mc.getMessage(UI_PACKAGEMANAGER_PACKAGE_NAME));
        packagesTable.setColumnHeader("displayVersion", mc.getMessage(UI_PACKAGEMANAGER_PACKAGE_VERSION));
        bar.addComponent(packagesTable);
        bar.setExpandRatio(packagesTable, 3.0f); // TreeTable should use as much space as it can - but doesn't

        view.getActionBar().addComponent(bar);
      }

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
  }
}

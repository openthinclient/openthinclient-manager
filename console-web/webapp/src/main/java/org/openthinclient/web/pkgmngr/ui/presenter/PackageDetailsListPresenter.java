package org.openthinclient.web.pkgmngr.ui.presenter;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.web.SchemaService;
import org.openthinclient.web.pkgmngr.ui.AffectedApplicationsSummaryDialog;
import org.openthinclient.web.pkgmngr.ui.InstallationPlanSummaryDialog;
import org.openthinclient.web.progress.ProgressReceiverDialog;

import java.util.Collection;

public class PackageDetailsListPresenter {

  public enum Mode {
    INSTALL, UNINSTALL, UPDATE
  }

  private final Mode mode;
  private final PackageActionOverviewPresenter packageActionOverviewPresenter;
  private final PackageManager packageManager;
  private final SchemaService schemaService;
  private final ApplicationService applicationService;

  public PackageDetailsListPresenter(Mode mode, PackageActionOverviewPresenter packageActionOverviewPresenter, PackageManager packageManager, SchemaService schemaService, ApplicationService applicationService) {
    this.mode = mode;
    this.packageActionOverviewPresenter = packageActionOverviewPresenter;
    packageActionOverviewPresenter.setMode(mode);
    this.packageManager = packageManager;
    this.schemaService = schemaService;
    this.applicationService = applicationService;

    // initially clear our views
    setPackages(null);

  }

  public void setPackages(Collection<org.openthinclient.pkgmgr.db.Package> otcPackages) {

    if (otcPackages == null || otcPackages.size() == 0) {
      // null or empty list indicate a reset of the view
      packageActionOverviewPresenter.hide();
      return;
    }

    packageActionOverviewPresenter.show();

    packageActionOverviewPresenter.setPackages(otcPackages);
    packageActionOverviewPresenter.onPerformAction(() -> {
      switch(mode) {
        case UPDATE:
          // fall-through
          // update is just a special case in the UI
        case INSTALL:
          doInstallPackages(otcPackages);
          break;
        case UNINSTALL:
          doUninstallPackages(otcPackages);
          break;
      }
    });
  }

  private void doInstallPackages(Collection<org.openthinclient.pkgmgr.db.Package> otcPackages) {
    final PackageManagerOperation op = packageManager.createOperation();
    otcPackages.forEach(op::install);
    op.resolve();

    // FIXME validate the state (Conflicts, missing packages, etc.)
    showSummaryDialog(op, () -> execute(op, true));

  }

  private void doUninstallPackages(Collection<org.openthinclient.pkgmgr.db.Package> otcPackages) {
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
}

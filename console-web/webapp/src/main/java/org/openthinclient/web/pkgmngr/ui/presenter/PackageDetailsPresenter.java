package org.openthinclient.web.pkgmngr.ui.presenter;

import static java.util.stream.Stream.concat;

import java.util.List;
import java.util.stream.Collectors;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.util.dpkg.PackageReferenceList;
import org.openthinclient.web.pkgmngr.ui.InstallationPlanSummaryDialog;
import org.openthinclient.web.progress.ProgressReceiverDialog;
import org.vaadin.viritin.button.MButton;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.ComponentContainer;

public class PackageDetailsPresenter {

    private final View view;
    private final PackageManager packageManager;

    public PackageDetailsPresenter(View view, PackageManager packageManager) {
        this.view = view;
        this.packageManager = packageManager;
    }

    public void setPackage(org.openthinclient.pkgmgr.db.Package otcPackage) {

        if (otcPackage != null) {
            view.show();
            view.setName(otcPackage.getName());
            view.setVersion(otcPackage.getVersion().toString());
            view.setDescription(otcPackage.getDescription());
            view.setShortDescription(otcPackage.getShortDescription());
            
            view.clearPackageList();
            // Check available and existing packages to match package-reference of current package
            // FIXME JN: es werden nur die Abhängigkeiten gegen die vorhandenen Pakete geprüft, was, 
            //           wenn eine Abhängigkeit ist die es in installable und installed nicht gibt?
            List<Package> installableAndExistingPackages = concat(
                packageManager.getInstalledPackages().stream(),
                packageManager.getInstallablePackages().stream()
            ).collect(Collectors.toList());

            PackageReferenceList depends = otcPackage.getDepends();
              installableAndExistingPackages.forEach(_package -> {
                if (depends.isReferenced(_package)) {
                  view.addPackage(_package);
                }
              });
            // -- 

            final ComponentContainer actionBar = view.getActionBar();

            actionBar.removeAllComponents();

            if (packageManager.isInstallable(otcPackage)) {
                actionBar.addComponent(new MButton("Install").withIcon(FontAwesome.DOWNLOAD).withListener(e -> {
                    doInstallPackage(otcPackage);
                }));
            }
            if (packageManager.isInstalled(otcPackage)) {
                actionBar.addComponent(new MButton("Uninstall").withIcon(FontAwesome.TRASH_O).withListener(e -> {
                    doUninstallPackage(otcPackage);
                }));
            }

        } else {
            view.hide();
        }

    }

    private void doUninstallPackage(Package otcPackage) {
        final PackageManagerOperation op = packageManager.createOperation();
        op.uninstall(otcPackage);
        op.resolve();

        // FIXME validate the state (Conflicts, missing packages, etc.)
        final InstallationPlanSummaryDialog summaryDialog = new InstallationPlanSummaryDialog(op.getInstallPlan(), false);
        summaryDialog.onInstallClicked(() -> execute(op, false));
        summaryDialog.open(true);
    }

    private void execute(PackageManagerOperation op, boolean install) {
        final ProgressReceiverDialog dialog = new ProgressReceiverDialog(install ? "Installation..." : "Uninstallation...");
        final ListenableProgressFuture<PackageManagerOperationReport> future = packageManager.execute(op);
        dialog.watch(future);

        dialog.open(true);
    }

    private void doInstallPackage(Package otcPackage) {
        final PackageManagerOperation op = packageManager.createOperation();
        op.install(otcPackage);
        op.resolve();

        // FIXME validate the state (Conflicts, missing packages, etc.)
        final InstallationPlanSummaryDialog summaryDialog = new InstallationPlanSummaryDialog(op.getInstallPlan());
        summaryDialog.onInstallClicked(() -> execute(op, true));
        summaryDialog.open(true);

    }

    public interface View {

        ComponentContainer getActionBar();

        void setName(String name);

        void setVersion(String version);

        void setDescription(String description);

        void hide();

        void show();
        
        void setShortDescription(String shortDescription);
        
        void addPackage(Package otcPackage);
        
        void clearPackageList();
    }
}

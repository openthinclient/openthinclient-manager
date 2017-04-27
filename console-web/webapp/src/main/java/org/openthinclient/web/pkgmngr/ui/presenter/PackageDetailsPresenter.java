package org.openthinclient.web.pkgmngr.ui.presenter;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.ComponentContainer;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.util.dpkg.PackageReference;
import org.openthinclient.util.dpkg.PackageReference.SingleReference;
import org.openthinclient.web.pkgmngr.ui.InstallationPlanSummaryDialog;
import org.openthinclient.web.pkgmngr.ui.view.AbstractPackageItem;
import org.openthinclient.web.pkgmngr.ui.view.ResolvedPackageItem;
import org.openthinclient.web.progress.ProgressReceiverDialog;
import org.vaadin.viritin.button.MButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Stream.concat;

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
            view.setSourceUrl(otcPackage.getSource().getUrl().toString());
            view.setChangeLog(otcPackage.getChangeLog());
            
            view.clearLists();
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
                  view.addDependency(new ResolvedPackageItem(_package));
                  isReferenced = true;
                  usedPackages.add(_package.getName());
                }
              }
              if (!isReferenced) {
                 if (pr instanceof SingleReference) {
                   view.addDependency(PackageDetailsUtil.createMissingPackageItem((SingleReference) pr));
                 }
              }
            }
            // --

            // conflicts
            if (otcPackage.getConflicts().isEmpty()) {
                view.hideConflictsTable();
            } else {
                for (PackageReference pr : otcPackage.getConflicts()) {
                    boolean isReferenced = false;
                    for (Package _package : installableAndExistingPackages) {
                        if (pr.matches(_package) && !usedPackages.contains(_package.getName())) {
                            view.addConflict(new ResolvedPackageItem(_package));
                            isReferenced = true;
                            usedPackages.add(_package.getName());
                        }
                    }
                    if (!isReferenced) {
                        if (pr instanceof SingleReference) {
                            view.addConflict(PackageDetailsUtil.createMissingPackageItem("", (SingleReference) pr));
                        }
                    }
                }
            }


            // provides
            if (otcPackage.getProvides().isEmpty()) {
                view.hideProvidesTable();
            } else {
                for (PackageReference pr : otcPackage.getProvides()) {
                    boolean isReferenced = false;
                    for (Package _package : installableAndExistingPackages) {
                        if (pr.matches(_package) && !usedPackages.contains(_package.getName())) {
                            view.addProvides(new ResolvedPackageItem(_package));
                            isReferenced = true;
                            usedPackages.add(_package.getName());
                        }
                    }
                    if (!isReferenced) {
                        if (pr instanceof SingleReference) {
                            view.addProvides(PackageDetailsUtil.createMissingPackageItem("", (SingleReference) pr));
                        }
                    }
                }
            }

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

        final InstallationPlanSummaryDialog summaryDialog = new InstallationPlanSummaryDialog(op, packageManager);
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
        final InstallationPlanSummaryDialog summaryDialog = new InstallationPlanSummaryDialog(op, packageManager);
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
        
        void addDependency(AbstractPackageItem rpi);

        void addConflict(AbstractPackageItem rpi);

        void addProvides(AbstractPackageItem rpi);

        void clearLists();
        
        void setSourceUrl(String url);
        
        void setChangeLog(String changeLog);

        void hideConflictsTable();

        void hideProvidesTable();
    }
}

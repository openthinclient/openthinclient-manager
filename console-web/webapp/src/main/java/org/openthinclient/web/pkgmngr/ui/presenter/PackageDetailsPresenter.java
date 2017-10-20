package org.openthinclient.web.pkgmngr.ui.presenter;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComponentContainer;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.web.pkgmngr.ui.InstallationPlanSummaryDialog;
import org.openthinclient.web.pkgmngr.ui.view.AbstractPackageItem;
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

            // Check available and existing packages to match package-reference of current package, sorted to use first matching package
            List<Package> installableAndExistingPackages = concat(
                packageManager.getInstalledPackages().stream(),
                packageManager.getInstallablePackages().stream()
            ).sorted()
             .collect(Collectors.toList());

            List<String> usedPackages = new ArrayList<>();
            view.addDependencies(PackageDetailsUtil.getReferencedPackageItems(otcPackage.getDepends(), installableAndExistingPackages, usedPackages));

            // conflicts
            if (otcPackage.getConflicts().isEmpty()) {
                view.hideConflictsTable();
            } else {
                view.addConflicts(PackageDetailsUtil.getReferencedPackageItems(otcPackage.getConflicts(), installableAndExistingPackages, usedPackages));
            }

            // provides
            if (otcPackage.getProvides().isEmpty()) {
                view.hideProvidesTable();
            } else {
                view.addProvides(PackageDetailsUtil.getReferencedPackageItems(otcPackage.getProvides(), installableAndExistingPackages, usedPackages));
            }

            final ComponentContainer actionBar = view.getActionBar();

            actionBar.removeAllComponents();

            if (packageManager.isInstallable(otcPackage)) {
                actionBar.addComponent(new MButton("Install").withIcon(VaadinIcons.DOWNLOAD).withListener((Button.ClickListener) e -> {
                    doInstallPackage(otcPackage);
                }));
            }
            if (packageManager.isInstalled(otcPackage)) {
                actionBar.addComponent(new MButton("Uninstall").withIcon(VaadinIcons.TRASH).withListener((Button.ClickListener) e -> {
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
        
        void addDependencies(List<AbstractPackageItem> apis);

        void addConflicts(List<AbstractPackageItem> apis);

        void addProvides(List<AbstractPackageItem> apis);

        void setSourceUrl(String url);
        
        void setChangeLog(String changeLog);

        void hideConflictsTable();

        void hideProvidesTable();
    }
}

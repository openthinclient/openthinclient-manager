package org.openthinclient.runtime.web.comptest.ui.tests;

import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.InstallPlan;
import org.openthinclient.pkgmgr.op.InstallPlanStep;
import org.openthinclient.web.pkgmngr.ui.InstallationPlanSummaryDialog;
import org.vaadin.viritin.button.MButton;

public class InstallationPlanSummaryDialogTest extends VerticalLayout implements ComponentTest {

    private final InstallationPlanSummaryDialog dialog;

    public InstallationPlanSummaryDialogTest() {
        setSpacing(true);

        final InstallPlan ip = new InstallPlan();
        dialog = new InstallationPlanSummaryDialog(ip);

        addComponent(new MButton("Open").withListener(e -> dialog.open(false)));
        addComponent(new MButton("Close").withListener(e -> dialog.close()));

        addComponent(new MButton("Add Install") //
                .withListener(e -> addInstallStep(ip)));
        addComponent(new MButton("Add Uninstall") //
                .withListener(e -> addUninstallStep(ip)));
        addComponent(new MButton("Add Update") //
                .withListener(e -> addUpdateStep(ip)));

    }

    private void addInstallStep(InstallPlan ip) {
        ip.getSteps().add(new InstallPlanStep.PackageInstallStep(createPackage("test-pkg", "1.2-3")));
        dialog.update();
    }

    private void addUninstallStep(InstallPlan ip) {
        ip.getSteps().add(new InstallPlanStep.PackageUninstallStep(createPackage("dumb-pkg", "0.1-21")));
        dialog.update();
    }

    private void addUpdateStep(InstallPlan ip) {
        ip.getSteps().add(new InstallPlanStep.PackageVersionChangeStep(
                createPackage("test-pkg", "1.2-3"),
                createPackage("old-pkg", "1.5-3")
        ));
        dialog.update();
    }

    private Package createPackage(String name, String version) {
        final Package pkg = new Package();
        pkg.setName(name);
        pkg.setVersion(version);
        return pkg;
    }

    @Override
    public String getTitle() {
        return "Installation Summary Dialog";
    }

    @Override
    public String getDetails() {
        return "Summary of Steps to be executed based on an InstallPlan.";
    }

    @Override
    public Component get() {
        return this;
    }
}

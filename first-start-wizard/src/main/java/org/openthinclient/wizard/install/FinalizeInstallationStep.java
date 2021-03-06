package org.openthinclient.wizard.install;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALL_FINALIZEINSTALLATIONSTEP_LABEL;

import org.openthinclient.api.context.InstallContext;
import org.openthinclient.manager.util.installation.InstallationDirectoryUtil;
import org.openthinclient.pkgmgr.PackageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

public class FinalizeInstallationStep extends AbstractInstallStep {
    private final Logger LOG = LoggerFactory.getLogger(FinalizeInstallationStep.class);

    @Override
    protected void doExecute(InstallContext installContext) throws Exception {

        final PackageManager pm = installContext.getPackageManager();

        if (pm != null) {
            LOG.info("Closing package manager instance");
            pm.close();
        }

        InstallationDirectoryUtil.removeInstallationFile(installContext.getManagerHome().getLocation().toPath());

        final ConfigurableApplicationContext context = installContext.getContext();
        if (context != null) {
            LOG.info("Closing temporary application context");
            context.close();
        }

    }

    @Override
    public String getName() {
        return mc.getMessage(UI_FIRSTSTART_INSTALL_FINALIZEINSTALLATIONSTEP_LABEL);
    }

    @Override
    public double getProgress() {
        return 1;
    }
}

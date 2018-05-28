package org.openthinclient.wizard.install;

import org.openthinclient.api.context.InstallContext;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.tftp.TFTPService;
import org.openthinclient.tftp.TFTPServiceConfiguration;

import java.nio.file.Path;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALL_CONFIGURETFTPINSTALLSTEP_LABEL;

public class ConfigureTFTPInstallStep extends AbstractInstallStep {
  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    final ManagerHome managerHome = installContext.getManagerHome();
    final TFTPServiceConfiguration configuration = managerHome.getConfiguration(TFTPServiceConfiguration.class);

    // FIXME validate that those paths are valid
    final Path tftpRootPath = managerHome.getLocation().toPath().resolve(TFTPService.DEFAULT_ROOT_PATH);

    // expose the root tftp directory
    final TFTPServiceConfiguration.Export export = new TFTPServiceConfiguration.Export();
    export.setPrefix("/");
    export.setBasedir(tftpRootPath.toString());
    configuration.getExports().add(export);

    // save the configuration
    managerHome.save(TFTPServiceConfiguration.class);
  }

  @Override
  public String getName() {
    return  mc.getMessage(UI_FIRSTSTART_INSTALL_CONFIGURETFTPINSTALLSTEP_LABEL);
  }

  @Override
  public double getProgress() {
    return 1;
  }
}

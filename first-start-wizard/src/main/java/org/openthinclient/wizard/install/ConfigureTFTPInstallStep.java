package org.openthinclient.wizard.install;

import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.tftp.PXEConfigTFTProvider;
import org.openthinclient.tftp.TFTPServiceConfiguration;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigureTFTPInstallStep extends AbstractInstallStep {
  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    final ManagerHome managerHome = installContext.getManagerHome();
    final TFTPServiceConfiguration configuration = managerHome.getConfiguration(TFTPServiceConfiguration.class);

    // FIXME validate that those paths are valid
    final Path tftpRootPath = managerHome.getLocation().toPath().resolve(Paths.get("nfs", "root", "tftp"));
    final Path templatePath = tftpRootPath.resolve("template.txt");

    // expose the root tftp directory
    final TFTPServiceConfiguration.Export export = new TFTPServiceConfiguration.Export();
    export.setPrefix("/");
    export.setBasedir(tftpRootPath.toString());
    configuration.getExports().add(export);

    // configure the pxelinux.cfg provider
    final TFTPServiceConfiguration.Export pxeExport = new TFTPServiceConfiguration.Export();
    pxeExport.setPrefix("/pxelinux.cfg");
    pxeExport.setProviderClass(PXEConfigTFTProvider.class);
    final TFTPServiceConfiguration.Export.Option templateOption = new TFTPServiceConfiguration.Export.Option();
    templateOption.setName("template");
    templateOption.setValue(templatePath.toString());
    pxeExport.getOptions().add(templateOption);
    configuration.getExports().add(pxeExport);

    // save the configuration
    managerHome.save(TFTPServiceConfiguration.class);
  }

  @Override
  public String getName() {
    return "Configure the TFTP exports";
  }
}

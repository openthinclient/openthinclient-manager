package org.openthinclient.wizard.install;

import org.openthinclient.api.context.InstallContext;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.service.nfs.NFSExport;
import org.openthinclient.service.nfs.NFSServiceConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALL_CONFIGURENFSINSTALLSTEP_LABEL;

public class ConfigureNFSInstallStep extends AbstractInstallStep {
  public static void main(String[] args) {


    final ConfigureNFSInstallStep step = new ConfigureNFSInstallStep();
    final InstallContext context = new InstallContext();
    context.setManagerHome(new ManagerHomeFactory().create());
    step.execute(context);

  }

  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    final ManagerHome managerHome = installContext.getManagerHome();

    final NFSServiceConfiguration nfsServiceConfiguration = managerHome.getConfiguration(NFSServiceConfiguration.class);

    // the following code creates the following default configuration:
//    <nfsexport spec="${jboss.server.data.dir}/nfs/root|/openthinclient|*(ro)" />
//    <nfsexport spec="${jboss.server.data.dir}/nfs/home|/home|*(rw)" />

    if (!containsExport(nfsServiceConfiguration, "/openthinclient"))
      nfsServiceConfiguration.getExports().add(createExport(managerHome, "/openthinclient", Paths.get("nfs", "root")));
    else
      log.info("Skipping /openthinclient export. Such an export already exists");
    if (!containsExport(nfsServiceConfiguration, "/home"))
      nfsServiceConfiguration.getExports().add(createExport(managerHome, "/home", Paths.get("nfs", "home")));
    else
      log.info("Skipping /home export. Such an export already exists");

    log.info("Listing all configured exports...");
    nfsServiceConfiguration.getExports().forEach(export -> {
      log.info("Export {}", export);

      if (!export.getRoot().exists()) {
        log.info("Root directory doesn't exist: '{}'. Directory will be created.", export.getRoot());
        export.getRoot().mkdirs();
      }

    });

    managerHome.save(NFSServiceConfiguration.class);

  }

  private boolean containsExport(NFSServiceConfiguration nfsServiceConfiguration, String exportName) {
    return nfsServiceConfiguration.getExports().stream()
            .filter(export -> export.getName().equals(exportName))
            .findFirst().isPresent();
  }

  private NFSExport createExport(ManagerHome managerHome, String name, Path relativePath) {
    final NFSExport export = new NFSExport();
    export.setName(name);
    final File root = managerHome.getLocation().toPath().resolve(relativePath).toFile();

    export.setRoot(root);
    final NFSExport.Group wildcardGroup = createWildcardGroup();
    export.getGroups().add(wildcardGroup);
    return export;
  }

  private NFSExport.Group createWildcardGroup() {
    final NFSExport.Group wildcardGroup = new NFSExport.Group();
    wildcardGroup.setWildcard(true);
    wildcardGroup.setReadOnly(false);
    return wildcardGroup;
  }

  @Override
  public String getName() {
    return mc.getMessage(UI_FIRSTSTART_INSTALL_CONFIGURENFSINSTALLSTEP_LABEL);
  }

  @Override
  public double getProgress() {
    return 1;
  }
}

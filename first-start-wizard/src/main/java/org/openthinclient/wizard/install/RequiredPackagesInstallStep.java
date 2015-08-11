package org.openthinclient.wizard.install;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.util.dpkg.Package;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RequiredPackagesInstallStep extends AbstractInstallStep {

  private final InstallableDistribution installableDistribution;

  public RequiredPackagesInstallStep(InstallableDistribution installableDistribution) {
    this.installableDistribution = installableDistribution;
  }

  @Override
  public String getName() {
    return "Install the required base packages";
  }

  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    final PackageManager packageManager = installContext.getPackageManager();

    final Collection<Package> installablePackages = packageManager.getInstallablePackages();

    final List<String> minimumPackages = installableDistribution.getMinimumPackages();
    final List<Optional<Package>> resolvedPackages = resolvePackages(installablePackages, minimumPackages);


    // verify that all packages have been resolved
    final List<String> missingPackages = new ArrayList<>();
    for (int i = 0; i < minimumPackages.size(); i++) {

      if (resolvedPackages.get(i).isPresent()) {
        final Package p = resolvedPackages.get(i).get();
        log.info("Installing package '{}', version '{}'", p.getName(), p.getVersion());
      } else {
        final String packageName = minimumPackages.get(i);
        missingPackages.add(packageName);
        log.error("No package found with name '{}'", packageName);
      }

    }

    // FIXME better error handling
    if (missingPackages.size() > 0)
      throw new IllegalStateException("Missing required packages: " + missingPackages);

    log.info("==============================================\n" +
            " starting OS install\n" +
            "==============================================\n");
    packageManager.install(resolvedPackages.stream().map(Optional::get).collect(Collectors.toList()));

  }

  protected List<Optional<Package>> resolvePackages(Collection<Package> installablePackages, List<String> minimumPackages) {
    return minimumPackages
            .stream()
            .map(pkgName -> installablePackages
                    .stream()
                    .filter(p -> pkgName.equals(p.getName()))
                    .sorted((p1, p2) -> {
                      // compare the version number
                      return p2.getVersion().compareTo(p1.getVersion());
                    })
                    .findFirst())
            .collect(Collectors.toList());
  }

}

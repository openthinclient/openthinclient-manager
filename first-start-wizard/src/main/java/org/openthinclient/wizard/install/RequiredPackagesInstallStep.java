package org.openthinclient.wizard.install;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALL_REQUIREDPACKAGESINSTALLSTEP_LABEL;

import java.util.*;
import org.openthinclient.api.context.InstallContext;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.progress.ListenableProgressFuture;

public class RequiredPackagesInstallStep extends AbstractInstallStep {

  private final InstallableDistribution installableDistribution;
  private boolean applicationIsPreview;

  private ListenableProgressFuture<PackageManagerOperationReport> future = null;

  public RequiredPackagesInstallStep(InstallableDistribution installableDistribution, boolean applicationIsPreview) {
    this.installableDistribution = installableDistribution;
    this.applicationIsPreview = applicationIsPreview;
  }

  @Override
  public String getName() {
    return mc.getMessage(UI_FIRSTSTART_INSTALL_REQUIREDPACKAGESINSTALLSTEP_LABEL);
  }

    @Override
    public double getProgress() {
      if (future == null) {
          return 0;
      } else {
          return future.getProgress();
      }
    }

  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    final PackageManager packageManager = installContext.getPackageManager();

    final Collection<Package> installablePackages = packageManager.getInstallablePackages();

    log.info("Installable packages:");
    installablePackages.forEach(pkg -> log.info(" - {}", pkg.getName()));

    final List<String> minimumPackages = installableDistribution.getMinimumPackages();
    final Map<String , Package> resolvedPackages = resolvePackages(installablePackages, minimumPackages);

    // verify that all packages have been resolved
    final List<String> missingPackages = new ArrayList<>();
    for (String packageName: minimumPackages) {
      if (resolvedPackages.containsKey(packageName)) {
        final Package p = resolvedPackages.get(packageName);
        log.info("Installing package '{}', version '{}'", p.getName(), p.getVersion());
      } else {
        missingPackages.add(packageName);
        log.error("No package found with name '{}'", packageName);
      }
    }

    // add additional packages to installation
    installableDistribution.getAdditionalPackages().forEach(p -> {
        Optional<Package> packageOptional = resolvePackage(installablePackages, p);
        if (packageOptional.isPresent()) {
            Package pkg = packageOptional.get();
            resolvedPackages.put(pkg.getName(), pkg);
            log.info("Installing package '{}', version '{}'", p.getName(), p.getVersion());
        } else {
            missingPackages.add(p.getName());
            log.error("No package found with name '{}', version '{}'", p.getName(), p.getVersion());
        }
    });

    // FIXME better error handling
    if (missingPackages.size() > 0)
      throw new IllegalStateException("Missing required packages: " + missingPackages);

    log.info("Resolving dependencies");
    final PackageManagerOperation operation = packageManager.createOperation();

    resolvedPackages.values().forEach(operation::install);

    operation.resolve();

    // handle conflicting packages
    if (!operation.getConflicts().isEmpty()) {
      for (PackageManagerOperation.PackageConflict conflict : operation.getConflicts()) {
        log.error("Found conflict: {} conflicts {}", conflict.getSource().toStringWithNameAndVersion(), conflict.getConflicting().toStringWithNameAndVersion());
      }
      throw new IllegalStateException("Detected conflicting packages.");
    }

    // handle unresolved packages
    if (!operation.getUnresolved().isEmpty()) {
      for (PackageManagerOperation.UnresolvedDependency unresolvedDependency : operation.getUnresolved()) {
        log.error("Found unresolved {} requires '{}'", unresolvedDependency.getSource().toStringWithNameAndVersion(), unresolvedDependency.getMissing());
      }
      throw new IllegalStateException("Detected unresolved packages.");
    }

    // handle suggested packages
    if (!operation.getSuggested().isEmpty()) {
      for (Package suggestedPackage : operation.getSuggested()) {
        log.error("Found suggested package for installation: {}", suggestedPackage.toStringWithNameAndVersion());
      }
      throw new IllegalStateException("Detected suggested packages.");
    }

    final StringBuilder sb = new StringBuilder();

    operation.getInstallPlan().getPackageInstallSteps().forEach(step -> {
      sb.append("  - ").append(step.getPackage().getName()).append("\n");
    });


    log.info("\n\n==============================================\n" +
            " starting OS install\n" +
            " \n"+
            " The final package list for the installation:\n" +
            sb.toString() +
            "==============================================\n\n");

//    final ListenableProgressFuture<PackageManagerOperationReport> future = packageManager.execute(operation);
    future = packageManager.execute(operation);

     // FIXME there should be some kind of smarter logic including org.openthinclient.progress presentation, etc.
     future.get();

    }

  protected Map<String, Package> resolvePackages(Collection<Package> installablePackages, List<String> minimumPackages) {
    Map<String, Package> result = new HashMap<>();
    for (Package pkg: installablePackages) {
      String name = pkg.getName();
      if(!minimumPackages.contains(name)) {
        continue;
      }
      if (!result.containsKey(name)) {
        result.put(name, pkg);
      } else {
        Version best_ver = result.get(name).getVersion();
        Version pkg_ver = pkg.getVersion();
        if(!applicationIsPreview && pkg_ver.isPreview() && !best_ver.isPreview()) {
          continue; // Skip preview versions if we got a stable version (unless this is a preview server)
        }
        // Prefer stable version, even if it's older (unless this is a preview server)
        boolean preferStable = (!applicationIsPreview && !pkg_ver.isPreview() && best_ver.isPreview());
        if(preferStable || pkg_ver.compareTo(best_ver) > 0) {
          result.put(name, pkg);
        }
      }
    }
    return result;
  }

  protected Optional<Package> resolvePackage(Collection<Package> installablePackages, Package pkg) {
      if (pkg.getVersion() == null) {
          return  installablePackages
                  .stream()
                  .filter(p -> pkg.getName().equals(p.getName()))
                  .sorted((p1, p2) -> {
                      // compare the version number
                      return p2.getVersion().compareTo(p1.getVersion());
                  })
                  .findFirst();
      } else {
          return installablePackages
                  .stream()
                  .filter(p -> pkg.getName().equals(p.getName()) && pkg.getVersion().equals(p.getVersion()))
                  .findFirst();
      }
    }
}

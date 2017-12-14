package org.openthinclient.wizard.install;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALL_REQUIREDPACKAGESINSTALLSTEP_LABEL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openthinclient.api.context.InstallContext;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.InstallPlanStep;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.progress.ListenableProgressFuture;

public class RequiredPackagesInstallStep extends AbstractInstallStep {

  private final InstallableDistribution installableDistribution;

  private ListenableProgressFuture<PackageManagerOperationReport> future = null;

  public RequiredPackagesInstallStep(InstallableDistribution installableDistribution) {
    this.installableDistribution = installableDistribution;
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

     // add additional packages to installation
     installableDistribution.getAdditionalPackages().forEach(p -> {
         Optional<Package> packageOptional = resolvePackage(installablePackages, p);
         if (packageOptional.isPresent()) {
             resolvedPackages.add(packageOptional);
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

    resolvedPackages.stream().map(Optional::get).forEach(operation::install);

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

    final List<InstallPlanStep> steps = operation.getInstallPlan().getSteps();
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

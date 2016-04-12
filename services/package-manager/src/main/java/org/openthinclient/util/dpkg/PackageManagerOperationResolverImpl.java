package org.openthinclient.util.dpkg;

import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.InstallPlan;
import org.openthinclient.pkgmgr.op.InstallPlanStep;
import org.openthinclient.pkgmgr.op.InstallPlanStep.PackageInstallStep;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.PackageConflict;
import org.openthinclient.pkgmgr.op.PackageManagerOperationResolver;
import org.openthinclient.util.dpkg.PackageReference.SingleReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

public class PackageManagerOperationResolverImpl implements PackageManagerOperationResolver {
  private static final Logger LOG = LoggerFactory.getLogger(PackageManagerOperationResolverImpl.class);
  private final Supplier<Collection<Package>> installedPackagesSupplier;
  private final Supplier<Collection<Package>> availablePackagesSupplier;

  public PackageManagerOperationResolverImpl(Supplier<Collection<Package>> installedPackagesSupplier,
                                             Supplier<Collection<Package>> availablePackagesSupplier) {
    this.installedPackagesSupplier = installedPackagesSupplier;
    this.availablePackagesSupplier = availablePackagesSupplier;
  }

  @Override
  public ResolveState resolve(Collection<Package> packagesToInstall, Collection<Package> packagesToUninstall) {


    final Collection<Package> installedPackages = installedPackagesSupplier.get();
    final Collection<Package> availablePackages = availablePackagesSupplier.get();

    // verify that the packages, that shall be installed, are actually known to the package manager. If one of those packages is unknown -> fail
    for (Package pkg : packagesToInstall) {
      if (!availablePackages.stream().anyMatch(available -> isSamePackage(pkg, available))) {
        // the package is not known to
        throw new PackageManagerException("Not a valid package: " + pkg);
      }
    }

    final ResolveState resolveState = new ResolveState();

    // phase 1: check if one of the packages to be installed is already installed in a different version
    findPackageChanges(packagesToInstall, installedPackages) //
            .forEach(resolveState.getInstallPlan().getSteps()::add);

    // phase 2: process the packages to be uninstalled
    findPackagesToUninstall(packagesToUninstall, installedPackages) //
            .forEach(resolveState.getInstallPlan().getSteps()::add);

    // phase 3: find packages to be installed. This is done using an exclusion based on the packages to be changed
    findPackagesToInstall(packagesToInstall, resolveState.getInstallPlan()) //
            .forEach(resolveState.getInstallPlan().getSteps()::add);


    // FIXME find and add dependencies
    findDependenciesToInstall(resolveState.getInstallPlan(), installedPackages, availablePackages, resolveState.getUnresolved()) //
            .forEach(resolveState.getInstallPlan().getSteps()::add);

    // phase 5: about conflicts
    checkInstallConflicts(resolveState.getInstallPlan(), installedPackages, resolveState.getConflicts());

    return resolveState;
  }

  /**
   * TODO: Gibt es Konflikte bei 'uninstall'?
   *       Version-Changes müssen berücksichtigt werden: es sollten keine Pakete gelöscht werden, die von irgendeinem anderen Paket benötigt werden
   *       
   *       
   * Prüfe, ob alle zu installierenden Pakete (installPlan) in keinem Konflikt mit bestehenden oder zu installierenden Paketen stehen
   * 
   * @param installPlan
   * @param conflicts
   */
  private void checkInstallConflicts(InstallPlan installPlan, Collection<Package> installedPackages, Collection<PackageConflict> conflicts) {
    
     List<Package> installableAndExistingPackages = concat(
            // newly installed packages
            installPlan.getPackageInstallSteps().map(InstallPlanStep.PackageInstallStep::getPackage),
          concat(
            // package with new version
            installPlan.getPackageVersionChangeSteps().map(InstallPlanStep.PackageVersionChangeStep::getTargetPackage),
            // existing packages
            installedPackages.stream()
          )
     ).collect(Collectors.toList());
      
     // remove unistall-packages from processing-list because they will not cause installation-conflicts
     List<Package> unistallPackages = installPlan.getPackageUninstallSteps().map(InstallPlanStep.PackageUninstallStep::getInstalledPackage).collect(Collectors.toList());
     installableAndExistingPackages.removeAll(unistallPackages);
     // remove installed packages marked for version change, because they will not cause installation-conflicts
     List<Package> versionChangeUnistall = installPlan.getPackageVersionChangeSteps().map(InstallPlanStep.PackageVersionChangeStep::getInstalledPackage).collect(Collectors.toList());
     installableAndExistingPackages.removeAll(versionChangeUnistall);
     
     // process conflicts for new install packages (including version change) 
     concat(
         installPlan.getPackageInstallSteps().map(InstallPlanStep.PackageInstallStep::getPackage),
         installPlan.getPackageVersionChangeSteps().map(InstallPlanStep.PackageVersionChangeStep::getTargetPackage)
     ).collect(Collectors.toList()).forEach(installPackage -> {
         installPackage.getConflicts().forEach(packageReference -> {
           
           conflicts.addAll(packageReferenceMatches(installPackage, packageReference, installableAndExistingPackages));
           
         });
     });
    
     // process conflicts for uninstall (including version change) packages
//     installableAndExistingPackages.forEach(iePackage -> {
//       iePackage.getDepends().forEach(packageReference -> {
//         unistallPackages.forEach(unistallPackage -> {
//           
//           conflicts.addAll(packageReferenceMatches(unistallPackage, packageReference, installableAndExistingPackages));
//         });
//         
//       });
//     });
     
     
  }

  /**
   * Checkt ob Paket in install-liste ist
   * @param source
   * @param conflictPackageReference
   * @param installableAndExistingPackages
   * @return
   */
  private Collection<PackageConflict> packageReferenceMatches(Package source, PackageReference conflictPackageReference, List<Package> installableAndExistingPackages) {

    return installableAndExistingPackages.stream()
                    .filter(pck -> conflictPackageReference.matches(pck))
                    .map(pck -> new PackageConflict(source, pck))
                    .collect(Collectors.toList());
  }

  /**
   * Ziel: Dependencies ermitteln: 1. Welche Dependencies 2. Berücksichtigung der Versionen der
   * Dependencies: -> Wenn Dependencie in atueller Version vorhanden ist: OK -> Wenn neuere Version
   * Vorhanden: prüfung ob das ok ist -> Wenn ältere VErsion vorhanden: updaten 3. Berücksichtigung
   * von Konflikten für die jeweils zu installierende Dependency
   */
  private Stream<InstallPlanStep> findDependenciesToInstall(InstallPlan installPlan, Collection<Package> installedPackages,
                                                    Collection<Package> availablePackages, Collection<PackageManagerOperation.UnresolvedDependency> unresolved) {

    final List<InstallPlanStep> dependenciesToInstall = new ArrayList<>();

    // resolve dependencies for
    Stream.concat(
            // newly installed packages
            installPlan.getPackageInstallSteps().map(InstallPlanStep.PackageInstallStep::getPackage),
            // and packages that shall be installed in a different version
            installPlan.getPackageVersionChangeSteps().map(InstallPlanStep.PackageVersionChangeStep::getTargetPackage)
    ).forEach(packageToInstall -> {
              final List<Package> dependencies = resolveDependencies(packageToInstall, installedPackages, availablePackages, unresolved);
              
              // FIXME add dependencies to install plan
              dependencies.stream().map(InstallPlanStep.PackageInstallStep::new)
                                   .forEach(dependenciesToInstall::add);
              // FIXME resolve dependencies of the dependencies
    });


//    LOG.debug("packagesToInstall {} has dependenciesToInstall {}", packagesToInstall, dependenciesToInstall);

    return dependenciesToInstall.stream();
  }

  private List<Package> resolveDependencies(Package packageToInstall, Collection<Package> installedPackages, Collection<Package> availablePackages, Collection<PackageManagerOperation.UnresolvedDependency> unresolved) {
    final List<Package> dependenciesToInstall = new ArrayList<>();
    PackageReferenceList depends = packageToInstall.getDepends();
    depends.forEach(packageReference -> {
      LOG.debug("packageToInstall {} depends {}", packageToInstall, packageReference);
      if (packageReference instanceof SingleReference) {

        SingleReference singleReference = (SingleReference) packageReference;
        Optional<Package> findFirst = installedPackages.stream().filter(singleReference::matches).findFirst();
        if (!findFirst.isPresent()) {
          // passendes Paket nicht installiert, prüfung auf unpassende Version (älter oder neu) -> upgrade/downgrade des singleReference (dependency

          // falls kein upgrade/downgrade dann install: hole packet aus availablePackages (das neueste, je nachdem was in)
          Optional<Package> findFirst2 = availablePackages.stream().filter(singleReference::matches).sorted().findFirst();
          if (findFirst2.isPresent()) {
            dependenciesToInstall.add(findFirst2.get());
          } else {
            // hier die Liste mit nicht erfüllen (nicht installierbaren) Abhängigkeiten
            unresolved.add(new PackageManagerOperation.UnresolvedDependency(packageToInstall, packageReference));
          }
        }

      } else {
        // TODO: handle OrReference: do we have 'real' test casees

      }

    });
    return dependenciesToInstall;
  }


  protected Stream<InstallPlanStep> findPackagesToInstall(Collection<Package> packagesToInstall,
                                                          InstallPlan installPlan) {

    return packagesToInstall.stream()
            // filter all packages that are not yet part of the installation
            .filter(pkg -> !isPartOfInstallPlan(pkg, installPlan))
            // all packages that are not filtered out will now become a install step
            .map(InstallPlanStep.PackageInstallStep::new);

  }

  protected boolean isPartOfInstallPlan(Package pkg, InstallPlan installPlan) {

    return installPlan.getSteps().stream().anyMatch(step -> {
      if (step instanceof InstallPlanStep.PackageInstallStep) {

        // check if the package is already mentioned in an install step
        return isSamePackage(pkg, ((InstallPlanStep.PackageInstallStep) step).getPackage());
      } else if (step instanceof InstallPlanStep.PackageVersionChangeStep) {

        // check if the package is mentioned in the version change.

        final InstallPlanStep.PackageVersionChangeStep change = (InstallPlanStep.PackageVersionChangeStep) step;

        return isSamePackage(pkg, change.getInstalledPackage()) || isSamePackage(pkg, change.getTargetPackage());
      }
      // XXX ignoring the uninstall right now
      return false;
    });
  }

  private Stream<InstallPlanStep> findPackagesToUninstall(Collection<Package> packagesToUninstall,
                                                          Collection<Package> installedPackages) {
    final List<Package> result = new ArrayList<>();
    for (Package packageToUninstall : packagesToUninstall) {

      final Optional<Package> matchingPackage = installedPackages.stream().filter(
              installedPackage -> isSamePackage(packageToUninstall, installedPackage)).findFirst();

      if (!matchingPackage.isPresent())
        LOG.warn("Found no matching installed package for {} {} to be uninstalled", packageToUninstall.getName(),
                packageToUninstall.getVersion());
      else
        result.add(matchingPackage.get());
    }

    return result.stream().map(InstallPlanStep.PackageUninstallStep::new);
  }

  protected boolean isSamePackage(Package pkg, Package other) {
    LOG.trace("isSamePackage: ", pkg, other);
    return pkg == other
            || (pkg.getName().equals(other.getName()) && pkg.getVersion().equals(other.getVersion()));
  }

  protected Stream<InstallPlanStep> findPackageChanges(Collection<Package> packagesToInstall, Collection<Package> installedPackages) {

    return packagesToInstall.stream().flatMap(
            packageToInstall -> {
              final String name = packageToInstall.getName();
              return installedPackages.stream().filter(pkg -> {
                return pkg.getName().equals(name);
              }).map(
                      pkg -> new InstallPlanStep.PackageVersionChangeStep(pkg, packageToInstall));
            });
  }

  @Override
  public boolean isValid(ResolveState resolveState) {
    return false;
  }

}

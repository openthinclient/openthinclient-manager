package org.openthinclient.util.dpkg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.PackageChange;
import org.openthinclient.pkgmgr.op.PackageManagerOperationResolver;
import org.openthinclient.util.dpkg.PackageReference.SingleReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    final ResolveState resolveState = new ResolveState();

    // phase 1: check if one of the packages to be installed is already installed in a different version
    findPackageChanges(packagesToInstall, installedPackages) //
        .forEach(resolveState.getChanges()::add);

    // phase 2: process the packages to be uninstalled
    findPackagesToUninstall(packagesToUninstall, installedPackages) //
        .forEach(resolveState.getUninstalling()::add);

    // phase 3: find packages to be installed. This is done using an exclusion based on the packages to be changed
    findPackagesToInstall(packagesToInstall, resolveState.getChanges(), availablePackages) //
        .forEach(resolveState.getInstalling()::add);


    findDependenciesToInstall(packagesToInstall, installedPackages, availablePackages) //
                            .forEach(resolveState.getInstalling()::add);

   //throw new UnsupportedOperationException();
   // phase 5: what about conflicts
   
   return resolveState;
   }
   
   /**
   * Ziel: Dependencies ermitteln:
   * 1. Welche Dependencies
   * 2. Berücksichtigung der Versionen der Dependencies: 
   *    -> Wenn Dependencie in atueller Version vorhanden ist: OK
   *    -> Wenn neuere Version Vorhanden: prüfung ob das ok ist
   *    -> Wenn ältere VErsion vorhanden: updaten
   * 3. Berücksichtigung von Konflikten für die jeweils zu installierende Dependency
   * 
   * @param packagesToInstall
   * @param installedPackages
   * @param availablePackages
   * @return
   */
   private Stream<Package> findDependenciesToInstall(Collection<Package> packagesToInstall, Collection<Package> installedPackages,
                                      Collection<Package> availablePackages) {
   
         final List<Package> dependenciesToInstall = new ArrayList<>();
         
         packagesToInstall.forEach(packageToInstall -> {
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
                  }
               }

            } else {
               // TODO: handle OrReference: do we have 'real' test casees
               
            }
         });
       });
   
       LOG.debug("packagesToInstall {} has dependenciesToInstall {}", packagesToInstall, dependenciesToInstall);
   
       return dependenciesToInstall.stream();
   }


  private Stream<Package> findPackagesToInstall(Collection<Package> packagesToInstall,
      List<PackageChange> changes, Collection<Package> availablePackages) {

    return packagesToInstall.stream()
        .filter(packageToInstall -> changes.stream().anyMatch(
            change -> isSamePackage(packageToInstall, change.getRequested())))
        .map(packageToInstall -> availablePackages.stream().filter(
            pkg -> isSamePackage(packageToInstall, pkg)).findFirst())
        .filter(Optional::isPresent)
        .map(Optional::get);

  }

  private Stream<Package> findPackagesToUninstall(Collection<Package> packagesToUninstall,
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

    return result.stream();
  }

  protected boolean isSamePackage(Package pkg, Package other) {
    return pkg == other ||
        (pkg.getName().equals(other.getName())
            && pkg.getVersion().equals(
            other.getVersion()));
  }

  protected Stream<PackageChange> findPackageChanges(Collection<Package> packagesToInstall, Collection<Package> installedPackages) {
    
    return packagesToInstall.stream().flatMap(
        packageToInstall -> {
          final String name = packageToInstall.getName();
          return installedPackages.stream().filter(pkg -> {
            return pkg.getName().equals(name);
          }).map(
              pkg -> new PackageChange(pkg, packageToInstall));
        });
  }

  @Override
  public boolean isValid(ResolveState resolveState) {
    return false;
  }

}

package org.openthinclient.util.dpkg;

import static java.util.stream.Stream.concat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.pkgmgr.op.InstallPlan;
import org.openthinclient.pkgmgr.op.InstallPlanStep;
import org.openthinclient.pkgmgr.op.InstallPlanStep.PackageInstallStep;
import org.openthinclient.pkgmgr.op.InstallPlanStep.PackageUninstallStep;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.PackageConflict;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.UnresolvedDependency;
import org.openthinclient.pkgmgr.op.PackageManagerOperationResolver;
import org.openthinclient.util.dpkg.PackageReference.SingleReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

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

    checkUnsatisfiedDependencies(resolveState.getInstallPlan(), installedPackages, resolveState.getUnresolved());
    
    // phase 5: about conflicts
    checkInstallConflicts(resolveState.getInstallPlan(), installedPackages, resolveState.getConflicts());

    return resolveState;
  }

  /**
   * Resolve dependencies of probably unsatisfied dependencies after uninstallation of an package.
   * Check unsatisfied dependencies and remove entries form UnistallPlan if uninstallation leads to missing dependencies
   * @param installPlan the Install plan
   * @param installedPackages installed packages
   * @param unresolved add entries (missing dependencies) to unresolved list
   */
  private void checkUnsatisfiedDependencies(InstallPlan installPlan, Collection<Package> installedPackages, Collection<UnresolvedDependency> unresolved) {
    
    // remove unistall-packages from processing-list because they will not cause installation-conflicts
    List<Package> unistallPackages = installPlan.getPackageUninstallSteps().map(InstallPlanStep.PackageUninstallStep::getInstalledPackage).collect(Collectors.toList());
    ArrayList<Package> existingWithoutUnistalled = new ArrayList<>(installedPackages);
    existingWithoutUnistalled.removeAll(unistallPackages);
    // remove installed packages marked for version change
    List<Package> versionChangeUnistall = installPlan.getPackageVersionChangeSteps().map(InstallPlanStep.PackageVersionChangeStep::getInstalledPackage).collect(Collectors.toList());
    existingWithoutUnistalled.removeAll(versionChangeUnistall);
    
    existingWithoutUnistalled.forEach(pck -> {
      pck.getDepends().forEach(dependencyOfInstalledPackage -> {
       
          Optional<Package> resovedDependencyToExistingPackage = existingWithoutUnistalled.stream().filter(ewuPac -> dependencyOfInstalledPackage.matches(ewuPac)).findFirst();
          if (!resovedDependencyToExistingPackage.isPresent()) {
            LOG.debug(pck.toStringWithNameAndVersion() + " misses dependency " + dependencyOfInstalledPackage);
            unresolved.add(new UnresolvedDependency(pck, dependencyOfInstalledPackage));
          }
    
      });
    });
    
    
    // cleanup uninstallPlan if unresolved dependencies matches uninstallable packages
    List<PackageUninstallStep> toRemoveFromUnistallList = new ArrayList<>();
    unresolved.forEach(unresolvedDependecy -> {
      installPlan.getPackageUninstallSteps().forEach(us -> {
        if (unresolvedDependecy.getMissing().matches(us.getInstalledPackage())) {
          toRemoveFromUnistallList.add(us);
        }
      });
    });
    installPlan.getSteps().removeAll(toRemoveFromUnistallList);
  }

  /**
   * 
   * TODO: Prüfe, ob alle zu installierenden Pakete (installPlan) in keinem Konflikt mit bestehenden oder zu installierenden Paketen stehen
   * 
   * @param installPlan
   * @param conflicts
   */
  private void checkInstallConflicts(InstallPlan installPlan, Collection<Package> installedPackages, Collection<PackageConflict> conflicts) {
    
     List<Package> installableAndExistingPackages = createInstallabeAndExistingPackageList(installPlan, installedPackages);
      
     List<Package> packagesToInstall = concat(
         installPlan.getPackageInstallSteps().map(InstallPlanStep.PackageInstallStep::getPackage),
         installPlan.getPackageVersionChangeSteps().map(InstallPlanStep.PackageVersionChangeStep::getTargetPackage)
     ).collect(Collectors.toList());
    
     // process conflicts for new install packages (including version change) 
     packagesToInstall.forEach(installPackage -> {
         installPackage.getConflicts().forEach(packageReference -> {
           conflicts.addAll(packageReferenceMatches(installPackage, packageReference, installableAndExistingPackages));
         });
     });
     
     // process conflicts for already installed packages (but without removable packages) against new packages for installation
     installableAndExistingPackages.forEach(installedPackage -> {
       installedPackage.getConflicts().forEach(installedPackageConflict -> {
         conflicts.addAll(packageReferenceMatches(installedPackage, installedPackageConflict, packagesToInstall));
       });
     });
    
     // process conflicts for already installed packages (but without removable packages) against new packages 'provides'
     installableAndExistingPackages.forEach(installedPackage -> {
       
       installedPackage.getConflicts().forEach(installedPackageConflict -> {
         packagesToInstall.forEach(packageToInstall -> {
           // check if package to install 'provides' contains 'conflicting' packages from already installed packages
           if (packageToInstall.getProvides().contains(installedPackageConflict)) {
             conflicts.addAll(packageReferenceMatchesInProvides(installedPackage, installedPackageConflict, packagesToInstall));
           }
         });
       });
       // check if already installed packages 'provides' packages wich conflicts to new ones
       installedPackage.getProvides().forEach(installedPackageProvides -> {
         packagesToInstall.forEach(packageToInstall -> {
           // is there a packageRefenceneList which equals
           if (packageToInstall.getConflicts().contains(installedPackageProvides)) {
             LOG.debug(packageToInstall.forConflictsToString() + " 'conflicts' matches to installedPackage 'provides' " + installedPackage);
             conflicts.add(new PackageConflict(packageToInstall, installedPackage));
           }
         });
       });
       
     });     
     
     // cleanup installPlan if conflicts exists through provided packages
     List<PackageInstallStep> toRemoveFromInstallList = new ArrayList<>();
     for(PackageConflict packageConflict : conflicts) {
       Iterator<PackageInstallStep> iterator = installPlan.getPackageInstallSteps().iterator();
       while(iterator.hasNext()) {
         PackageInstallStep pis = iterator.next();
         // find the first matching entry for conflicting source in installList and mark for removal
         if (isSamePackage(packageConflict.getSource(), pis.getPackage()) || 
             // check if installPackages provides a conflict-matching reference
             isSourcePackageConflictsMatchesProvidedPackages(packageConflict.getSource(), pis.getPackage()) ) {
           
           toRemoveFromInstallList.add(pis);
           break; // if first matching entry was found
         }
       };
     };
     installPlan.getSteps().removeAll(toRemoveFromInstallList);     
  }

  /**
   * Compare source.conflicts and somePackage.provides
   * @param source Package with collection of Conflicts
   * @param somePackage Package with provides Package to search for in source
   * @return true if any element in 'somePackage.provides()' is contained in 'source.conflicts()'; otherwise returns false.
   */
  private boolean isSourcePackageConflictsMatchesProvidedPackages(Package source, Package somePackage) {
     PackageReferenceList conflicts = source.getConflicts();
     PackageReferenceList provides = somePackage.getProvides();
     return CollectionUtils.containsAny(conflicts, provides);
  }

  /**
   * Returns a list of {@link PackageConflict} if installableAndExistingPackages contains a matching package 
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
   * Returns a list of {@link PackageConflict} if installableAndExistingPackages contains a matching package 
   * @param source
   * @param conflictPackageReference
   * @param installableAndExistingPackages
   * @return
   */
  private Collection<PackageConflict> packageReferenceMatchesInProvides(Package source, PackageReference conflictPackageReference, List<Package> installableAndExistingPackages) {

    return installableAndExistingPackages.stream()
                    .filter(pck -> pck.getProvides().stream().filter(pckp -> pckp.equals(conflictPackageReference)).findAny().isPresent())
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

    
    List<Package> installableAndExistingPackages = createInstallabeAndExistingPackageList(installPlan, installedPackages);
    
    final List<InstallPlanStep> dependenciesToInstall = new ArrayList<>();

    // resolve dependencies for
    Stream.concat(
            // newly installed packages
            installPlan.getPackageInstallSteps().map(InstallPlanStep.PackageInstallStep::getPackage),
            // and packages that shall be installed in a different version
            installPlan.getPackageVersionChangeSteps().map(InstallPlanStep.PackageVersionChangeStep::getTargetPackage)
    ).forEach(packageToInstall -> {
              final List<Package> dependencies = resolveDependencies(packageToInstall, installableAndExistingPackages, availablePackages, unresolved);
              
              // FIXME add dependencies to install plan
              dependencies.stream().map(InstallPlanStep.PackageInstallStep::new)
                                   .forEach(dependenciesToInstall::add);
              // FIXME resolve dependencies of the dependencies
    });

//    // resolve dependencies of probably unsatisfied dependencies after uninstallation of an package
//    // TODO Das wird nur benötigt wenn ein Paktekt gelöscht wird, denn hier werden einfach nochmal alle Pakete (existierende abzgl. zu löschende) auf Dependencies gecheckt   
//    // remove unistall-packages from processing-list because they will not cause installation-conflicts
//    List<Package> unistallPackages = installPlan.getPackageUninstallSteps().map(InstallPlanStep.PackageUninstallStep::getInstalledPackage).collect(Collectors.toList());
//    ArrayList<Package> existingWithoutUnistalled = new ArrayList<>(installedPackages);
//    existingWithoutUnistalled.removeAll(unistallPackages);
//    
//    existingWithoutUnistalled.forEach(pck -> {
//      pck.getDepends().forEach(dependencyOfInstalled -> {
//        System.out.println("Exisisting "+pck.forConflictsToString());
//        
//      });
//    });

//    LOG.debug("packagesToInstall {} has dependenciesToInstall {}", packagesToInstall, dependenciesToInstall);

    return dependenciesToInstall.stream();
  }

  private List<Package> createInstallabeAndExistingPackageList(InstallPlan installPlan, Collection<Package> installedPackages) {
  
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
    
    return installableAndExistingPackages;
  }

  /**
   * 
   * @param packageToInstall
   * @param installableAndExistingPackages
   * @param availablePackages
   * @param unresolved
   * @return
   */
  private List<Package> resolveDependencies(Package packageToInstall, Collection<Package> installableAndExistingPackages, Collection<Package> availablePackages, Collection<PackageManagerOperation.UnresolvedDependency> unresolved) {
    final List<Package> dependenciesToInstall = new ArrayList<>();
    final List<PackageReference> providedDependencies = new ArrayList<>();
    
    PackageReferenceList depends = packageToInstall.getDepends();
    
    // berücksichtigung von 'replaces': wenn ein schon installiertes Paket eine benötigte Abhängigkeit erfüllt (hier 'replaced' hat),
    // braucht diese benötigte Abhängigkeit nicht mehr installiert werden 
    installableAndExistingPackages.forEach(installedPackage -> {
      installedPackage.getReplaces().forEach(installedReplacedPackageReference -> {
        processExistingPackageReference(providedDependencies, depends, installedReplacedPackageReference);
      });
    });
    
    
    // berücksichtigung von 'provides': wenn ein schon installiertes Paket eine benötigte Abhängigkeit erfüllt (hier 'provided' hat),
    // braucht diese benötigte Abhängigkeit nicht mehr installiert werden 
    installableAndExistingPackages.forEach(installedPackage -> {
      installedPackage.getProvides().forEach(installedProvidedPackageReference -> {
        processExistingPackageReference(providedDependencies, depends, installedProvidedPackageReference);
      });
    });

    depends.forEach(packageReference -> {
      LOG.debug("packageToInstall {} depends {}", packageToInstall, packageReference);
      
      if (!providedDependencies.contains(packageReference)) {
      
        if (packageReference instanceof SingleReference) {
  
          
          // wenn kein installiertes Paket die Abhängigkeite 'provides', dann suche nach installierbarem
          SingleReference singleReference = (SingleReference) packageReference;
          Optional<Package> findFirst = installableAndExistingPackages.stream().filter(singleReference::matches).findFirst();
          if (!findFirst.isPresent()) {
            // passendes Paket nicht installiert, prüfung auf unpassende Version (älter oder neu) -> upgrade/downgrade des singleReference (dependency
  
            
            // falls kein upgrade/downgrade dann install: hole packet aus availablePackages (das neueste, je nachdem was in)
            Optional<Package> findFirst2 = availablePackages.stream().filter(singleReference::matches).sorted((p1, p2) -> -p1.compareTo(p2)).findFirst();
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
      }

    });
    return dependenciesToInstall;
  }

  /**
   * This method checks if, existingPackageReference matches one entry of depends-list, if so: this entry will be added to providedDependencies 
   * @param providedDependencies
   * @param depends
   * @param existingPackageReference - PackageReference to an already existing (i.e. installed/provided) package
   */
  private void processExistingPackageReference(final List<PackageReference> providedDependencies, PackageReferenceList depends, PackageReference existingPackageReference) {
    if (existingPackageReference instanceof SingleReference) {
      SingleReference singleReference = (SingleReference) existingPackageReference;
      depends.forEach(dependsPackageReference -> {
        // FIXME: howto 'match' PackageReferences? 
        Package p = new Package();
        p.setName(singleReference.getName());
        if (singleReference.getVersion() != null) {
          p.setVersion(singleReference.getVersion());
        } else {
          p.setVersion(new Version()); // Schmerz!
        }        
        // ---
        if (dependsPackageReference.matches(p)) {
          providedDependencies.add(dependsPackageReference);
        }
      });
    } else {
      // TODO: handle OrReference: do we have 'real' test casees

    }
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
    return pkg == other || (pkg.getName().equals(other.getName()) && pkg.getVersion().equals(other.getVersion()));
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

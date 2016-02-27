package org.openthinclient.util.dpkg;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.PackageChange;
import org.openthinclient.pkgmgr.op.PackageManagerOperationResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
  public ResolveState resolve(Collection<Package> packagesToInstall,
      Collection<Package> packagesToUninstall) {

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


    // phase 4: resolve all dependencies for the packages to be installed
    //    findDependencies(packagesToInstall, installedPackages, availablePackages) //
    //        .forEach(resolveState.getInstalling()::add);

    throw new UnsupportedOperationException();
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

  protected Stream<PackageChange> findPackageChanges(
      Collection<Package> packagesToInstall, Collection<Package> installedPackages) {
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

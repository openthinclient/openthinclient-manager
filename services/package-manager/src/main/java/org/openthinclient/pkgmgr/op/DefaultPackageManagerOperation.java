package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.db.Package;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DefaultPackageManagerOperation implements PackageManagerOperation {

  private final List<Package> packagesToInstall;
  private final List<Package> packagesToUninstall;
  private final PackageManagerOperationResolver resolver;
  private PackageManagerOperationResolver.ResolveState resolveState;

  public DefaultPackageManagerOperation(PackageManagerOperationResolver resolver) {
    this.resolver = resolver;
    packagesToInstall = new ArrayList<>();
    packagesToUninstall = new ArrayList<>();
  }

  @Override
  public void install(Package pkg) {
    packagesToInstall.add(pkg);
  }

  @Override
  public void uninstall(Package pkg) {
    packagesToUninstall.add(pkg);
  }

  @Override
  public boolean isResolved() {
    return resolveState != null && resolver.isValid(resolveState);
  }

  @Override
  public void resolve() {
    resolveState = resolver.resolve(packagesToInstall, packagesToUninstall);
  }

  @Override
  public Collection<Package> getDependencies() {
    return resolveState != null ? resolveState.getInstalling() : Collections.emptyList();
  }

  @Override
  public Collection<Package> getSuggested() {
    return resolveState != null ? resolveState.getSuggested() : Collections.emptyList();
  }

  @Override
  public Collection<PackageConflict> getConflicts() {
    return resolveState != null ? resolveState.getConflicts() : Collections.emptyList();
  }

  public PackageManagerOperationResolver.ResolveState getResolveState() {
    return resolveState;
  }
}

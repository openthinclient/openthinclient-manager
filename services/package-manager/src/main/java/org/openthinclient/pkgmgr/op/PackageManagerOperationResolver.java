package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.db.Package;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface PackageManagerOperationResolver {

  ResolveState resolve(Collection<Package> packagesToInstall, Collection<Package> packagesToUninstall);

  /**
   * Checks if the given {@link ResolveState} is still valid since the creation. This method will only verify that there
   * have been no changes to the known package database since the last resolve
   *
   * @param resolveState
   * @return
   */
  boolean isValid(ResolveState resolveState);

  class ResolveState {
    private final List<Package> installing;
    private final List<Package> uninstalling;
    private final List<Package> suggested;
    private final List<PackageManagerOperation.PackageConflict> conflicts;
    private final List<PackageManagerOperation.PackageChange> changes;
    private final List<Package> installed;

    public ResolveState() {
      installing = new ArrayList<>();
      uninstalling = new ArrayList<>();
      suggested = new ArrayList<>();
      conflicts = new ArrayList<>();
      installed = new ArrayList<>();
      changes = new ArrayList<>();
    }

    public List<Package> getUninstalling() {
      return uninstalling;
    }

    public List<Package> getInstalling() {
      return installing;
    }

    public List<Package> getSuggested() {
      return suggested;
    }

    public Collection<PackageManagerOperation.PackageConflict> getConflicts() {
      return conflicts;
    }

    public List<PackageManagerOperation.PackageChange> getChanges() {
      return changes;
    }

    public List<Package> getInstalled() {
      return installed;
    }
  }

}

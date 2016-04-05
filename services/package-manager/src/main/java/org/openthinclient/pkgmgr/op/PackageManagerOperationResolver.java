package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.db.Package;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface PackageManagerOperationResolver {

    ResolveState resolve(Collection<Package> packagesToInstall, Collection<Package> packagesToUninstall);

    /**
     * Checks if the given {@link ResolveState} is still valid since the creation. This method will
     * only verify that there have been no changes to the known package database since the last
     * resolve
     */
    boolean isValid(ResolveState resolveState);

    class ResolveState {
        private final List<Package> suggested;
        private final List<PackageManagerOperation.PackageConflict> conflicts;
        private final InstallPlan installPlan;
        private final List<PackageManagerOperation.UnresolvedDependency> unresolved;

        public ResolveState() {
            suggested = new ArrayList<>();
            conflicts = new ArrayList<>();
            installPlan = new InstallPlan();
            unresolved = new ArrayList<>();
        }

        public InstallPlan getInstallPlan() {
            return installPlan;
        }

        public List<Package> getSuggested() {
            return suggested;
        }

        public Collection<PackageManagerOperation.PackageConflict> getConflicts() {
            return conflicts;
        }

        public Collection<PackageManagerOperation.UnresolvedDependency> getUnresolved() {
            return unresolved;
        }
    }

}

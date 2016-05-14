package org.openthinclient.pkgmgr.op;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.PackageManagerOperationResolver.ResolveState;
import org.openthinclient.util.dpkg.PackageReference;

import java.util.Collection;

public interface PackageManagerOperation {

    /**
     * Add a package that shall be installed. This will only add this single package, without
     * further resolving of dependencies or verification that no conflicts will be introduced.
     *
     * @param pkg the {@link Package} that shall be installed
     */
    void install(Package pkg);

    /**
     * Add a package that shall be uninstalled. If that package has been marked for {@link
     * #install(Package) installation}, this method will remove that package from the list of
     * packages to be installed.
     *
     * @param pkg the {@link Package} that shall be uninstalled
     */
    void uninstall(Package pkg);

    /**
     * Returns the resolve state of this operation. In case of <code>true</code> the {@link
     * PackageManagerOperation} all dependencies have been resolved.
     *
     * @return <code>true</code> if the operation has been {@link #resolve() resolved} and no
     * further {@link #install(Package) package installation} or {@link #uninstall(Package) package
     * uninstallation} tasks have been added
     */
    boolean isResolved();

    void resolve();

//    /**
//     * A readonly {@link Collection} of computed dependencies. Note that this {@link Collection} is
//     * only valid if {@link #resolve() it has been resolved}.
//     *
//     * @return a readonly {@link Collection} of computed dependencies
//     */
//    Collection<Package> getDependencies();

    /**
     * A readonly {@link Collection} containing packages suggested by one of the installed packages.
     * Note that this {@link Collection} is only valid if {@link #resolve() it has been resolved}.
     *
     * @return a readonly {@link Collection} containing packages suggested by one of the installed
     * packages.
     */
    Collection<Package> getSuggested();

    /**
     * A readonly {@link Collection} containing packages that are conflicts to packages to be
     * installed in this operation. Note that this {@link Collection} is only valid if {@link
     * #resolve() it has been resolved}.
     *
     * @return a readonly {@link Collection} containing packages that are conflicts to packages to
     * be installed in this operation.
     */
    Collection<PackageConflict> getConflicts();

    /**
     * A readonly {@link Collection} of {@link UnresolvedDependency} will be used to declare a dependency which cannot be resolved because it does not exist in available packages
     * OR because it will be deleted and depends to other packages
     */
    Collection<UnresolvedDependency> getUnresolved();
    
    /**
     * Returns the computed {@link InstallPlan}. Note that this {@link InstallPlan} is onaly valid if
     * {@link #resolve() this operations has been resolved}. <br> This method will return
     * <code>null</code> if the {@link PackageManagerOperation operation} has not been resolved yet.
     */
    InstallPlan getInstallPlan();

    /**
     * The state of resolve-process
     *
     * @return the ResolveStae containing detailed information about resolve process
     */
    ResolveState getResolveState();

    /**
     * Describes the conflict of a package to be installed.
     */
    class PackageConflict {
        /**
         * The package that is a conflict for {@link #source}.
         */
        private final Package conflicting;

        /**
         * The source package that states the conflict with {@link #conflicting}
         */
        private final Package source;

        public PackageConflict(Package source, Package conflicting) {
            this.source = source;
            this.conflicting = conflicting;
        }

        public Package getConflicting() {
            return conflicting;
        }

        public Package getSource() {
            return source;
        }
        
        @Override
        public String toString() {
          return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
              .append("source", source.forConflictsToString())
              .append("conflicting", conflicting.forConflictsToString())
              .toString();
        }        
    }

    /**
     * {@link UnresolvedDependency} contains information about missing dependencies for a specific
     * package.
     */
    class UnresolvedDependency {
        private final Package source;
        private final PackageReference missing;

        public UnresolvedDependency(Package source, PackageReference missing) {
            this.source = source;
            this.missing = missing;
        }

        /**
         * The {@link Package} that has a {@link #getMissing() unmatched dependency}
         */
        public Package getSource() {
            return source;
        }

        public PackageReference getMissing() {
            return missing;
        }
        
        @Override
        public String toString() {
          return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
              .append("source", source.forConflictsToString())
              .append("missing", missing)
              .toString();
        }        
    }
}

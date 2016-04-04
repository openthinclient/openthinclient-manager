package org.openthinclient.pkgmgr.op;

import java.util.Collection;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.PackageManagerOperationResolver.ResolveState;

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

    /**
     * A readonly {@link Collection} of computed dependencies. Note that this {@link Collection} is
     * only valid if {@link #resolve() it has been resolved}.
     *
     * @return a readonly {@link Collection} of computed dependencies
     */
    Collection<Package> getDependencies();

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

//    /**
//     * A readonly {@link Collection} of package references that could not be resolved.
//     */
//    Collection<PackageReference> getUnresolved();
    
    /**
     * The state of resolve-process
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
    }

    /**
     * Describes that change of a installed package to a different requested version.
     */
    class PackageChange {
        /**
         * The already installed version of the package.
         */
        private final Package installed;

        /**
         * The package to be installed. Although the name of this field suggests that the version
         * will be newer, this is not a strict requirement. This may also reqpresent a package
         * downgrade.
         */
        private final Package requested;

        public PackageChange(Package installed, Package requested) {
            this.installed = installed;
            this.requested = requested;
        }

        public Package getInstalled() {
            return installed;
        }

        public Package getRequested() {
            return requested;
        }
    }
}

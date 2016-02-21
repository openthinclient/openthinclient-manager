package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.db.Package;

import java.util.Collection;

public interface PackageManagerOperation {

    /**
     * Add a package that shall be installed. This will only add this single package, without further resolving of dependencies or verification that no conflicts will be introduced.
     *
     * @param pkg the {@link Package} that shall be installed
     */
    void install(Package pkg);

    /**
     * Add a package that shall be uninstalled. If that package has been marked for {@link #install(Package) installation}, this method will remove that package from the list of packages to be installed.
     *
     * @param pkg the {@link Package} that shall be uninstalled
     */
    void uninstall(Package pkg);


    /**
     * Returns the resolve state of this operation. In case of <code>true</code> the {@link PackageManagerOperation} all dependencies have been resolved.
     *
     * @return
     */
    boolean isResolved();

    void resolve();

    Collection<Package> getDependencies();

    Collection<Package> getSuggested();
}

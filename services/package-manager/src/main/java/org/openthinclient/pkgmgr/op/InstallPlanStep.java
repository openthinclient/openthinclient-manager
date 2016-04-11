package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.db.Package;

/**
 * Represents a single step during the execution of a specific {@link InstallPlan}. Each step
 * represents a specific action on the installed packages. For details about the different types of
 * steps, see the nested classes.
 */
public abstract class InstallPlanStep {

    /**
     * Represents the installation of a {@link Package}.
     */
    public static class PackageInstallStep extends InstallPlanStep {
        private final Package pkg;

        public PackageInstallStep(Package pkg) {
            this.pkg = pkg;
        }

        public Package getPackage() {
            return pkg;
        }
    }

    /**
     * Represents the up/downgrade of an already installed {@link Package}
     */
    public static class PackageVersionChangeStep extends InstallPlanStep {
        private final Package installedPackage;
        private final Package targetPackage;

        public PackageVersionChangeStep(Package installedPackage, Package targetPackage) {
            this.installedPackage = installedPackage;
            this.targetPackage = targetPackage;
        }

        /**
         * The {@link Package} that is currently installed. This version of the {@link Package}
         * shall be replaced by the {@link #getTargetPackage() target version}.
         *
         * @return {@link Package} that is currently installed.
         */
        public Package getInstalledPackage() {
            return installedPackage;
        }

        /**
         * The {@link Package} that shall replace the {@link #getInstalledPackage() installed
         * package}.
         *
         * @return the new {@link Package} to be installed
         */
        public Package getTargetPackage() {
            return targetPackage;
        }
    }

    /**
     * Represents the removal of a installed {@link Package}
     */
    public static class PackageUninstallStep extends InstallPlanStep {
        private final Package installedPackage;

        public PackageUninstallStep(Package installedPackage) {
            this.installedPackage = installedPackage;
        }

        /**
         * The installed {@link Package} that shall be removed.
         *
         * @return the installed {@link Package} to be removed
         */
        public Package getInstalledPackage() {
            return installedPackage;
        }
    }
}
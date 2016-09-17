package org.openthinclient.service.common.home.impl;

import org.openthinclient.service.common.home.ManagerHome;

import java.io.File;

public class ManagerHomeFactory {

    private File managerHomeDirectory;

    /**
     * Checks whether or not the manager home has been specified using a system property.
     *
     * @return <code>true</code> if the system property <code>manager.home</code> has been specified
     */
    public boolean isManagerHomeDefinedAsSystemProperty() {
        return getManagerHomeSystemProperty() != null;
    }

    private String getManagerHomeSystemProperty() {
        return System.getProperty("manager.home");
    }

    /**
     * Checks if a manager home directory is known and if that directory seems to contain a valid
     * installation.
     *
     * @return <code>true</code> if the manager home has been specified and seems to contain a valid
     * installation.
     */
    public boolean isManagerHomeValidAndInstalled() {

        final File managerHomeDirectory = getManagerHomeDirectory();
        if (managerHomeDirectory == null)
            return false;

        // FIXME this is basically a duplication of the code in CheckManagerHomeDirectory

        final File[] contents = managerHomeDirectory.listFiles(pathname -> {
            return
                    // ignore typical MacOS directories
                    !pathname.getName().equals(".DS_Store") &&
                            // the installer will create a system property logging.file which will point to a file in the logs directory.
                            !pathname.getName().equals("logs");
        });

        return contents != null && contents.length > 0;
    }

    /**
     * Returns the manager home base directory or <code>null</code>. This method will only return a
     * non-<code>null</code> value if either there has been a {@link #setManagerHomeDirectory(File)
     * preconfigured} manager home, or the <code>manager.home</code> system property has been set.
     *
     * @return a {@link File} pointing to the manager home directory or <code>null</code>
     */
    public File getManagerHomeDirectory() {

        if (this.managerHomeDirectory != null)
            return managerHomeDirectory;

        // check whether there has been a system property set
        final String value = getManagerHomeSystemProperty();

        if (value != null && value.trim().length() > 0) {
            return new File(value);
        }

        return null;
    }

    /**
     * Explicitly specify the manager home directory. This will override the
     * <code>manager.home</code> system property.
     *
     * @param baseDirectory the base directory to be used as the new manager home.
     */
    public void setManagerHomeDirectory(File baseDirectory) {
        if (baseDirectory == null) {
            throw new IllegalArgumentException("baseDirectory must not be null");
        }
        this.managerHomeDirectory = baseDirectory;
    }

    public ManagerHome create() {

        final File managerHomeDirectory = getManagerHomeDirectory();

        if (managerHomeDirectory == null) {
            throw new IllegalStateException("No manager home directory has been specified");
        }

        return new DefaultManagerHome(managerHomeDirectory);
    }

}

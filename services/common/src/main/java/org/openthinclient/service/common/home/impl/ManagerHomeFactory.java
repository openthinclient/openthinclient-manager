package org.openthinclient.service.common.home.impl;

import org.openthinclient.service.common.home.ManagerHome;

import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ManagerHomeFactory {


  public static final String KEY_MANAGER_HOME = "manager-home";

  /**
   * Checks if a manager home directory is known and if that directory seems to contain a valid installation.
   * @return <code>true</code> if the manager home has been specified and seems to contain a valid installation.
   */
  public boolean isManagerHomeValidAndInstalled() {

    final File managerHomeDirectory = getManagerHomeDirectory();
    if (managerHomeDirectory == null)
      return false;

    // FIXME this is basically a duplication of the code in CheckManagerHomeDirectory

    final File[] contents = managerHomeDirectory.listFiles(pathname -> {
      // ignore typical MacOS directories
      return !pathname.getName().equals(".DS_Store");
    });

    return contents != null && contents.length > 0;
    }

  /**
   * Returns the manager home base directory or <code>null</code>.
   * This method will only return a non-<code>null</code> value if either there has been a preconfigured manager home, or the <code>manager.home</code> system property has been set.
   *
   * @return a {@link File} pointing to the manager home directory or <code>null</code>
   */
  public File getManagerHomeDirectory() {

    final Preferences preferences = getPreferences();

    final String preconfiguredManagerHome = preferences.get(KEY_MANAGER_HOME, null);

    if (preconfiguredManagerHome != null) {
      return new File(preconfiguredManagerHome);
    }

    // check whether there has been a system property set
    final String value = System.getProperty("manager.home");

    if (value != null && value.trim().length() > 0) {
      return new File(value);
    }

    return null;
  }

  /**
   * Specify the manager home directory. This setting will override the manager home system property, and the value will be persisted using {@link Preferences}.
   *
   * @param baseDirectory the base directory to be used as the new manager home.
   */
  public void setManagerHomeDirectory(File baseDirectory) {
    if (baseDirectory == null) {
      throw new IllegalArgumentException("baseDirectory must not be null");
    }
    final Preferences preferences = getPreferences();
    preferences.put(KEY_MANAGER_HOME, baseDirectory.getAbsolutePath());
    try {
      preferences.flush();
    } catch (BackingStoreException e) {
      throw new RuntimeException("Failed to store manager home directory location.", e);
    }
  }

  private Preferences getPreferences() {
    return Preferences.userRoot().node("org").node("openthinclient").node("manager");
  }

  public ManagerHome create() {

    final File managerHomeDirectory = getManagerHomeDirectory();

    if (managerHomeDirectory == null) {
      throw new IllegalStateException("No manager home directory has been specified");
    }

    return new DefaultManagerHome(managerHomeDirectory);
  }

}

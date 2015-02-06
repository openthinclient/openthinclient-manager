package org.openthinclient.pkgmgr.connect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openthinclient.pkgmgr.PackageManagerException;

import com.levigo.util.preferences.PreferenceStoreHolder;
import com.levigo.util.preferences.PropertiesPreferenceStore;

public class InitProperties {
  private static PreferenceStoreHolder prefStHo = PreferenceStoreHolder.getInstance();
  private static String tempStoreName = "tempPackageManager";
  private static volatile boolean propertiesInitialized;

  public static void ensurePropertiesInitialized() throws PackageManagerException {
    synchronized (InitProperties.class) {
      if (!propertiesInitialized) {
        doInitializeProperties();
        propertiesInitialized = true;
      }
    }
  }

  private static void doInitializeProperties() throws PackageManagerException {
    String programRootDirectory = System.getProperty("jboss.server.data.dir");
    String propertiesFileName;
    String configDir;
    PropertiesPreferenceStore prefStore;
    propertiesFileName = "package_manager.properties";
    configDir = new File(
        programRootDirectory + File.separator + "nfs" + File.separator + "root" + File.separator + "etc"
            + File.separator).getPath();
    prefStore = new PropertiesPreferenceStore();
    try {
      InputStream stream = null;
      if ((new File(configDir, propertiesFileName)).isFile()
          && (new File(configDir, propertiesFileName)).length() != 0L) {
        stream = new FileInputStream(new File(configDir, propertiesFileName));
      }
      if (stream == null) {
        final ClassLoader aClassLoader = PreferenceStoreHolder.class.getClassLoader();
        if (aClassLoader == null) {
          stream = ClassLoader.getSystemResourceAsStream(propertiesFileName);
        } else {
          stream = aClassLoader.getResourceAsStream(propertiesFileName);
        }
        if (stream == null) {
          final Class aClass = PreferenceStoreHolder.class;
          stream = aClass.getResourceAsStream(propertiesFileName);
        }
        if (stream == null) {
          if ((new File(propertiesFileName)).length() != 0L) {
            stream = new FileInputStream(propertiesFileName);
          } else {
            throw new PackageManagerException(
                "FATAL ERROR the file " + propertiesFileName + " which should be located in the " + configDir
                    + " could not be loaded");
          }
        }
      }
      if (stream != null) {
        prefStore.load(stream);
        PreferenceStoreHolder.addPreferenceStoreByName("PackageManager", prefStore);
        stream.close();
        final PropertiesPreferenceStore tempPrefStore = new PropertiesPreferenceStore();
        tempPrefStore.putPreference("installDir", (new StringBuilder()).append(getRealPath(programRootDirectory,
            prefStHo.getPreferenceAsString("PackageManager", "installDir", null))).append(File.separator).toString());
        tempPrefStore.putPreference("workingDir", (new StringBuilder()).append(getRealPath(programRootDirectory,
            prefStHo.getPreferenceAsString("PackageManager", "workingDir", null))).append(File.separator).toString());
        tempPrefStore.putPreference("archivesDir", (new StringBuilder()).append(getRealPath(programRootDirectory,
            prefStHo.getPreferenceAsString("PackageManager", "archivesDir", null))).append(File.separator).toString());
        tempPrefStore.putPreference("testinstallDir", (new StringBuilder()).append(getRealPath(programRootDirectory,
            prefStHo.getPreferenceAsString("PackageManager", "testinstallDir", null))).append(
            File.separator).toString());
        tempPrefStore.putPreference("partialDir", (new StringBuilder()).append(getRealPath(programRootDirectory,
            prefStHo.getPreferenceAsString("PackageManager", "partialDir", null))).append(File.separator).toString());
        tempPrefStore.putPreference("listsDir", (new StringBuilder()).append(getRealPath(programRootDirectory,
            prefStHo.getPreferenceAsString("PackageManager", "listsDir", null))).append(File.separator).toString());
        tempPrefStore.putPreference("packageDB",
            getRealPath(programRootDirectory, prefStHo.getPreferenceAsString("PackageManager", "packageDB", null)));
        tempPrefStore.putPreference("cacheDB",
            getRealPath(programRootDirectory, prefStHo.getPreferenceAsString("PackageManager", "cacheDB", null)));
        tempPrefStore.putPreference("sourcesList",
            getRealPath(programRootDirectory, prefStHo.getPreferenceAsString("PackageManager", "sourcesList", null)));
        tempPrefStore.putPreference("installOldDir",
            getRealPath(programRootDirectory, prefStHo.getPreferenceAsString("PackageManager", "installOldDir", null)));
        tempPrefStore.putPreference("oldDB",
            getRealPath(programRootDirectory, prefStHo.getPreferenceAsString("PackageManager", "oldDB", null)));
        tempPrefStore.putPreference("removeItReally", getRealPath(programRootDirectory,
            prefStHo.getPreferenceAsString("PackageManager", "removeItReally", null)));
        tempPrefStore.putPreference("archivesDB",
            getRealPath(programRootDirectory, prefStHo.getPreferenceAsString("PackageManager", "archivesDB", null)));
        if (prefStHo.isAccessible()) {
          PreferenceStoreHolder.removePreferenceStore(tempStoreName);
        }
        PreferenceStoreHolder.addPreferenceStoreByName(tempStoreName, tempPrefStore);
        new ProxyProperties(prefStHo.getPreferenceAsString("PackageManager", "proxyHost", null),
            prefStHo.getPreferenceAsString("PackageManager", "proxyPort", null),
            prefStHo.getPreferenceAsString("PackageManager", "proxyUser", null),
            prefStHo.getPreferenceAsString("PackageManager", "proxyPass", null),
            prefStHo.getPreferenceAsBoolean("proxyInUse", false));
      }
      return;
    } catch (final IOException x) {
      x.printStackTrace();
      throw new PackageManagerException(x);
    }
  }

  /**
   * put both given strings together, check if it's an existing file an return
   * the canonical path of it
   *
   * @param programRootDirectory
   * @param path
   * @return the canonical path
   * @throws IOException
   */
  private static String getRealPath(String programRootDirectory, String path) throws IOException {
    File f = new File(path);
    if (!f.isAbsolute())
      f = new File(programRootDirectory, path);
    return f.getAbsoluteFile().getCanonicalPath();
  }
}

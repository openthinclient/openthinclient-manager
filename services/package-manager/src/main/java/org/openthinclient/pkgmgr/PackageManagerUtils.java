package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.db.Package;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * PackageManagerUtils has util methods to support Package handling
 */
public class PackageManagerUtils {

  /**
   * Reduces the given list to only the latest version of each given package
   * @param packages - the packages to reduce
   * @return reduced list of packages with the latest stable version, and
   *         additional packages with the latest preview version, if they are
   *         newer than the latest stable version or no stable version exists).
   */
  public List<Package> reduceToLatestVersion(List<Package> packages) {

    Map<String, Package> releaseSeen = new HashMap<>();
    Map<String, Package> previewSeen = new HashMap<>();

    for (Package pkg : packages) {
      Map<String, Package> seen = pkg.getVersion().isPreview()? previewSeen : releaseSeen;
      if (!seen.containsKey(pkg.getName())) {
        seen.put(pkg.getName(), pkg);
      } else {
        Package p = seen.get(pkg.getName());
        if (p.getVersion().compareTo(pkg.getVersion()) <= 0) {
          seen.replace(pkg.getName(), pkg);
        }
      }
    }
    List<Package> result = new ArrayList<>();
    result.addAll(releaseSeen.values());

    for (Map.Entry<String, Package> entry : previewSeen.entrySet()) {
      String name = entry.getKey();
      Package pkg = entry.getValue();
      if (!releaseSeen.containsKey(name)
          || releaseSeen.get(name).getVersion().compareTo(pkg.getVersion()) < 0) {
        result.add(pkg);
      }
    }

    return result;
  }

  /**
   * Parse given string with format 'package-name_version-number' and creates an package object
   * @param p string with expected format
   * @return Package
   */
  public static Package parse(String p) {
    Package pkg;
    // parse package declaration with format: packageName-2.1-1
    if (p.split("_\\d").length > 1) {
      int separatorIdx = p.indexOf("_");
      String name = p.substring(0, separatorIdx);
      String version = p.substring(separatorIdx + 1);
      pkg = createPackage(name, version);
    } else {
      pkg = createPackage(p, null);
    }
    return pkg;
  }

  /**
   * Create a Packgae-object with name and version
   * @param name package-name
   * @param version package-version
   * @return Package
   */
  public static Package createPackage(String name, String version) {
    final Package pkg = new Package();
    pkg.setName(name);
    if (version != null) {
      pkg.setVersion(version);
    }
    return pkg;
  }

}

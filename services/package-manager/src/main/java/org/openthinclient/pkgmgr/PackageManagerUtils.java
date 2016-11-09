package org.openthinclient.pkgmgr;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.openthinclient.pkgmgr.db.Package;

/**
 * PackageManagerUtils has util methods to support Package handling
 */
public class PackageManagerUtils {

  /**
   * Reduces the given list to only the latest version of each given package
   * @param packages - the packages to reduce
   * @return - the list of reduces packages, each package has the latest version
   */
  public List<Package> reduceToLatestVersion(List<Package> packages) {
    
    Map<String, Package> seen = new ConcurrentHashMap<>();
    
    for (Package pkg : packages) {
      if (!seen.containsKey(pkg.getName())) {
        seen.put(pkg.getName(), pkg);
      } else {
        Package p = seen.get(pkg.getName());
        if (p.getVersion().compareTo(pkg.getVersion()) < 1) {
          seen.replace(pkg.getName(), pkg);
        }
      }
    }
   
    return seen.entrySet().stream().map(p -> p.getValue()).collect(Collectors.toList());
  }

}

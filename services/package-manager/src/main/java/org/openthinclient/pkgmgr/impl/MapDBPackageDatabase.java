package org.openthinclient.pkgmgr.impl;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.openthinclient.pkgmgr.PackageDatabase;
import org.openthinclient.pkgmgr.PackageDatabaseFactory;
import org.openthinclient.util.dpkg.Package;
import org.openthinclient.util.dpkg.PackageReference;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapDBPackageDatabase implements PackageDatabase {
  private final DB db;
  private final NavigableSet<Package> packages;

  public MapDBPackageDatabase(DB db, NavigableSet<Package> packages) {
    this.db = db;
    this.packages = packages;
  }

  @Override
  public void save() throws IOException {
    db.commit();
  }

  @Override
  public void close() {
    db.close();
  }

  @Override
  public boolean isPackageInstalled(String name) {
    final Optional<Package> result = getPackageByName(name);
    return result.isPresent();
  }

  /**
   * This method will traverse all packages in this database and find the newest (in regards to the version) Package contained.
   *
   * @param name
   * @return
   */
  private Optional<Package> getPackageByName(String name) {
    return packages.stream() //
            .filter(pkg -> pkg.getName().equals(name)) //
            .sorted(this::comparePackageVersionDesc)
            .findFirst();
  }

  /**
   * Creates a Comparator that sorts the packages in descending order. That is, the highest version will come first.
   *
   * @return
   */
  private int comparePackageVersionDesc(Package pkg1, Package pkg2) {
    return pkg2.getVersion().compareTo(pkg1.getVersion());
  }

  @Override
  public boolean isPackageInstalledDontVerifyVersion(String name) {
    // verify what version?
    return isPackageInstalled(name);
  }

  @Override
  public Map<String, Package> getProvidedPackages() {

    final Map<String, Package> latestPackageVersions = getLatestPackageVersions();

    // re-add all packages that do provide another "virtual" package
    Map<String, List<Package>> virtualPackages = new HashMap<>();

    // search for all packages providing another virtual package
    // this intermediate step is required to collect all package versions.
    // The newest version for a provided package will be selected in the next step.
    packages.stream()
            .filter(this::hasProvidedPackages)
            .forEach(pkg -> {
              getProvidedPackages(pkg).forEach(ref -> {

                        List<Package> packages = virtualPackages.get(ref.getName());

                        if (packages == null) {
                          packages = new ArrayList<>();
                          virtualPackages.put(ref.getName(), packages);
                        }

                        packages.add(pkg);
                      }
              );
            });

    // process all virtual packages and select the newest package version of the package list.
    // this package will then be added to the latestPackageVersions map
    virtualPackages.forEach((pkgName, packages) ->
                    // there is no need to check whether getNewestPackageVersion returns null, as
                    // if there is an entry in the map, at least one package will be in the list.
                    latestPackageVersions.put(pkgName, getNewestPackageVersion(packages))
    );

    return latestPackageVersions;
  }

  private boolean hasProvidedPackages(Package pkg) {
    return pkg.getProvides() != null && getProvidedPackages(pkg).findFirst().isPresent();
  }

  private Stream<PackageReference.SingleReference> getProvidedPackages(Package pkg) {
    if (pkg.getProvides() != null) {
      pkg.getProvides().stream().filter(e -> e instanceof PackageReference.SingleReference).map(e -> (PackageReference.SingleReference) e);
    }

    return Stream.empty();
  }

  /**
   * Constructs a Map containing only the latest versions of the packages.
   *
   * @return
   */
  private Map<String, Package> getLatestPackageVersions() {
    final Map<String, List<Package>> groupedByPackageName = getGroupedByPackageName();

    return groupedByPackageName.entrySet().stream()
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> getNewestPackageVersion(e.getValue())
            ));
  }

  /**
   * Selects the newest package version in the given list of packages. This method will not verify
   * whether or not the package names are all equal. Comparison will be performed solely based on
   * the package versions
   *
   * @param packages a {@link List} of {@link Package packages}
   * @return the {@link Package} with newest version
   */
  private Package getNewestPackageVersion(List<Package> packages) {

    // shortcut: if there is only one single package in the list, there is no need to process the list using a stream
    if (packages.size() == 1)
      return packages.get(0);

    final Optional<Package> result = packages.stream().sorted(this::comparePackageVersionDesc).findFirst();
    if (result.isPresent())
      return result.get();
    return null;
  }

  private Map<String, List<Package>> getGroupedByPackageName() {
    return packages.stream()
            .collect(Collectors.groupingBy(Package::getName));
  }

  @Override
  public Collection<Package> getPackages() {
    return packages;
  }

  @Override
  public void addPackage(Package newPkg) {


    final Iterator<Package> iter = packages.iterator();
    while (iter.hasNext()) {
      Package pkg = iter.next();
      // the existing package has the same name

      if (pkg.getName().equals(newPkg.getName())) {
        // and the existing package has an older version than the new package
        if (comparePackageVersionDesc(pkg, newPkg) >= 0) {
          // remove the existing package and add the new replacement.
          iter.remove();
          packages.add(newPkg);
          return;
        }
      }
    }

    packages.add(newPkg);
  }

  @Override
  public void addPackageDontVerifyVersion(Package pkg) {
    packages.add(pkg);
  }

  @Override
  public Package getPackage(String name) {
    final Optional<Package> result = packages.stream() //
            .filter(pkg -> pkg.getName().equals(name)) //
            .sorted(this::comparePackageVersionDesc) //
            .findFirst();
    if (result.isPresent())
      return result.get();
    return null;
  }

  @Override
  public List<Package> getProvidesPackages(String provided) {

    return packages.stream()
            .filter(pkg -> getProvidedPackages(pkg).anyMatch(ref -> ref.getName().equals(provided)))
            .collect(Collectors.toList());
  }

  @Override
  public List<Package> getDependency(Package pack) {

    return packages.stream()
            .filter(pkg -> pkg.getDepends().isReferenced(pack) || pkg.getPreDepends().isReferenced(pack))
            .collect(Collectors.toList());
  }

  @Override
  public boolean removePackage(Package pkg) {
    // XXX note: this implementation is due to compatibility reasons with the old serialization db. Currently the version of the package is not taken into account, when deleting an existing package.
    return packages.removeIf(e -> e.getName().equals(pkg.getName()));
  }

  public static class MapDBPackageDatabaseFactory implements PackageDatabaseFactory {

    @Override
    public PackageDatabase create(Path targetPath) throws IOException {

      final DB db = DBMaker
              .newFileDB(targetPath.toFile())
              .checksumEnable()
              .closeOnJvmShutdown()
              .make();

      final NavigableSet<Package> packages = db.new BTreeSetMaker("packages") //
              // enabling the counter, as updates will not happen that often, but access will
              .counterEnable()
              .makeOrGet();

      return new MapDBPackageDatabase(db, packages);

    }
  }
}

package org.openthinclient.pkgmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface PackageRepository extends JpaRepository<Package, Integer> {

  Package getByNameAndVersion(String name, Version version);

  List<Package> getByName(String name);

  //  @Query("select p from Package p, PackageState ps join ps.pkg where ps.state='INSTALLED'")
  @Query("select ps.pkg from PackageState ps join ps.pkg where ps.state='INSTALLED'")
  List<Package> getInstalledPackages();

  @Query("select ps.pkg from PackageState ps join ps.pkg where ps.state='UNINSTALLED'")
  List<Package> getUninstalledPackages();

}

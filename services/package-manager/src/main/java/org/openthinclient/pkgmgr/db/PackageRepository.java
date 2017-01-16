package org.openthinclient.pkgmgr.db;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

@Component
public interface PackageRepository extends JpaRepository<Package, Integer> {

    Package getByNameAndVersion(String name, Version version);

    List<Package> getByName(String name);

    @Query("select p from Package p where p.installed = false and p.source.enabled = true")
    List<Package> findInstallablePackages(); 
    
    List<Package> findByInstalledTrue();

    Package getBySourceAndNameAndVersion(Source source, String name, Version version);

    List<Package> findBySource(Source source);

}

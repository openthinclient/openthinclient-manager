package org.openthinclient.pkgmgr.db;

import org.openthinclient.pkgmgr.db.Package.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface PackageRepository extends JpaRepository<Package, Integer> {

    Package getByNameAndVersion(String name, Version version);

    List<Package> getByName(String name);

    List<Package> findByInstalledFalse();

//    List<Package> findByInstalledFalseAndStatus(Status status);

    List<Package> findByInstalledTrue();

    Package getBySourceAndNameAndVersion(Source source, String name, Version version);

    Package getByNameAndVersionAndStatus(String name, Version version, Status status);

}

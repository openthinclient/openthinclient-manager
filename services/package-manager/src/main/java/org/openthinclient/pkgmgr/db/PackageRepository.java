package org.openthinclient.pkgmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface PackageRepository extends JpaRepository<Package, Integer> {

    Package getByNameAndVersion(String name, Version version);

    List<Package> getByName(String name);

}

package org.openthinclient.pkgmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageRepository extends JpaRepository<Package, Integer> {

    Package getByNameAndVersion(String name, Version version);

    List<Package> getByName(String name);

    @Query("select p from Package p where p.installed = false and p.source.enabled = true")
    List<Package> findInstallablePackages();

    List<Package> findByInstalledTrue();

    @Query(value = "select p from Package p where p.source=?1 and p.name=?2  and version_epoch=?3 and version_upstream=?4 and version_revision = ?5")
    Package getBySourceAndNameAndVersionWithRevision(Source source, String name, Integer epoch, String upstream, String revision);

    @Query(value = "select p from Package p where p.source=?1 and p.name=?2  and version_epoch=?3 and version_upstream=?4 and version_revision is null")
    Package getBySourceAndNameAndVersionWithRevisionIsNull(Source source, String name, Integer epoch, String upstream);

    List<Package> findBySource(Source source);

}

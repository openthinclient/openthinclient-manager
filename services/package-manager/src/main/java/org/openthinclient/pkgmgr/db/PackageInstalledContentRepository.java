package org.openthinclient.pkgmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageInstalledContentRepository extends JpaRepository<PackageInstalledContent, Long> {

    List<PackageInstalledContent> findByPkg(Package pkg);

    List<PackageInstalledContent> findByPkgOrderBySequenceDesc(Package pkg);
}

package org.openthinclient.pkgmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageInstalledContentRepository extends JpaRepository<PackageInstalledContent, Long> {

}

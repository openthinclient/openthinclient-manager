package org.openthinclient.pkgmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PackageUninstalledContentRepository extends JpaRepository<PackageUninstalledContent, Long> {

    @Transactional
    void deleteByPath(String path);
}

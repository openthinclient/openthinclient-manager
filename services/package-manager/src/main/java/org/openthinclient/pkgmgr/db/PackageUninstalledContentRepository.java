package org.openthinclient.pkgmgr.db;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PackageUninstalledContentRepository extends JpaRepository<PackageUninstalledContent, Long> {

    @Transactional
    void deleteByPath(String path);

    @Query("SELECT p FROM PackageUninstalledContent p WHERE p.path LIKE '%.sfs'")
    List<PackageUninstalledContent> findAllSFContent();

    List<PackageUninstalledContent> findAllByPackageIdOrderBySequenceAsc(Long packageId);
}

package org.openthinclient.pkgmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * A spring data repository used to access the {@link Source} entries in the database.
 */
@Repository
public interface SourceRepository extends JpaRepository<Source, Long> {

}

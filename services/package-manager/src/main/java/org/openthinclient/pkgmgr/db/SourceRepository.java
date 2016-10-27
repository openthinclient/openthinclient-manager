package org.openthinclient.pkgmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

/**
 * A spring data repository used to access the {@link Source} entries in the database.
 */
@Component
public interface SourceRepository extends JpaRepository<Source, Long> {

}

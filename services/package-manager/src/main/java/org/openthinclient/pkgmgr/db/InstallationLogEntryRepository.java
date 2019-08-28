package org.openthinclient.pkgmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstallationLogEntryRepository extends JpaRepository<InstallationLogEntry, Integer> {

  List<InstallationLogEntry> findByInstallation(Installation installation);

  // FIXME when packages are installed multiple times, the most recent installation should be determined. - should not happen
  // FIXME is there a way to alias pkg to package? - obsolete
  // unused: List<InstallationLogEntry> findByPkg(Package pkg);

}

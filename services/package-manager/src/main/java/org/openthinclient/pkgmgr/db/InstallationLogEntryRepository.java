package org.openthinclient.pkgmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface InstallationLogEntryRepository extends JpaRepository<InstallationLogEntry, Integer> {

  List<InstallationLogEntry> findByInstallation(Installation installation);

  // FIXME when packages are installed multiple times, the most recent installation should be determined.
  // FIXME is there a way to alias pkg to package?
  List<InstallationLogEntry> findByPkg(Package pkg);

  //    List<InstallationLogEntry> findByPkg(Package pkg);

}

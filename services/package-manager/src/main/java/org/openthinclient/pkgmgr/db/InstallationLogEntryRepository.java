package org.openthinclient.pkgmgr.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface InstallationLogEntryRepository extends JpaRepository<InstallationLogEntry, Integer> {

   List<InstallationLogEntry> findByInstallation(Installation installation);

//    List<InstallationLogEntry> findByPackage(Package pkg);

}

package org.openthinclient.pkgmgr;

import java.nio.file.*;
import java.util.*;

import javax.annotation.PostConstruct;

import org.openthinclient.pkgmgr.db.PackageUninstalledContent;
import org.openthinclient.pkgmgr.db.PackageUninstalledContentRepository;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Clean up uninstalled packages' contents. if they are no longer in use.
 *
 * This component is instrumented by UninstalledPackagesCleanerConfiguration.
 * Usage is as follows:
 *  - startCollection() is called to prepare for a new collection round
 *  - addMountedPaths() is called to add mounted package paths (reported by
 *    clients via WebSocket)
 *  - runCleanup() is called to perform the actual cleanup (called after a
 *    timeout to allow clients to report their mounted packages)
 */
@Component
public class UninstalledPackagesCleaner {
  private static final
  Logger LOG = LoggerFactory.getLogger(UninstalledPackagesCleaner.class);

  @Autowired
  private PackageUninstalledContentRepository uninstalledContentRepo;

  private Path nfsRootPath;
  private boolean collecting = false;
  private Set<String> mountedPackagePaths = new HashSet<>();

  @PostConstruct
  public void init() {
    Path managerHome = Paths.get(
      (new ManagerHomeFactory()).getManagerHomeDirectory().getAbsolutePath());
    nfsRootPath = managerHome.resolve("nfs/root");
  }

  public synchronized void startCollection() {
    collecting = true;
    mountedPackagePaths.clear();
  }

  public synchronized void addMountedPaths(Collection<String> paths) {
    LOG.info("Received mounted package paths: {}", paths);
    if (!collecting) return;
    LOG.info("Adding mounted package paths");
    mountedPackagePaths.addAll(paths);
  }

  /**
  * Get SFS paths from uninstalled packages, ignore those still mounted,
  * get all uninstalled paths from uninstalledContentRepository and delete
  * them.
  */
  public synchronized void runCleanup() {
    collecting = false;
    LOG.info("Cleaning up uninstalled packages.");

    for (PackageUninstalledContent uninstalledContent:
            uninstalledContentRepo.findAllSFContent()) {
      String sfsPath = uninstalledContent.getPath();
      if (mountedPackagePaths.contains(sfsPath)) continue;

      Long pkgId = uninstalledContent.getPackageId();
      List<PackageUninstalledContent> contents =
            uninstalledContentRepo.findAllByPackageIdOrderBySequenceAsc(pkgId);
      for (PackageUninstalledContent content: contents) {
        Path path = nfsRootPath.resolve(content.getPath());
        try {
          LOG.info("Deleting uninstalled {}", path);
          Files.deleteIfExists(path);
        } catch (DirectoryNotEmptyException e) {
          LOG.warn("Directory {} not empty, cannot delete.", path);
          // user interfered, forget entry (below), don't try to clean up again
        } catch (SecurityException e) {
          LOG.warn("No permission to delete {}", path);
          continue;  // keep db entry for later retry
        } catch (Exception e) {
          LOG.error("Failed to delete uninstalled " + path, e);
          continue;  // keep db entry for later retry
        }
        uninstalledContentRepo.delete(content);
      }
    }
    LOG.info("Finished cleaning up uninstalled packages.");
  }
}

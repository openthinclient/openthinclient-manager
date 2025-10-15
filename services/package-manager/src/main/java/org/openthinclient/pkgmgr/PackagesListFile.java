package org.openthinclient.pkgmgr;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.openthinclient.pkgmgr.db.PackageUninstalledContent;
import org.openthinclient.pkgmgr.db.PackageUninstalledContentRepository;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component("packagesListFile")
public class PackagesListFile {
  private static final
  Logger LOG = LoggerFactory.getLogger(PackagesListFile.class);

  private Path nfsRootPath;
  private Path packagesPath;
  private Path packagesListPath;
  private Path tempPackagesListPath;

  @Autowired
  PackageUninstalledContentRepository uninstalledContentTable;

  @Autowired
  private PackageManager packageManager;

  private long updateAt = Long.MAX_VALUE;
  private Thread updateRequestProcessor;
  private final static long UPDATE_DELAY = 500; // ms

  private WatchService watchService = null;

  @PostConstruct
  public void init() {
    Path managerHome = Paths.get(
        (new ManagerHomeFactory()).getManagerHomeDirectory().getAbsolutePath());
    nfsRootPath = managerHome.resolve("nfs/root");
    packagesPath          = nfsRootPath.resolve("packages");
    packagesListPath      = nfsRootPath.resolve("packages.list");
    tempPackagesListPath  = nfsRootPath.resolve("packages.list.tmp");

    // Ensure packages/ exists
    if (!Files.isDirectory(packagesPath)) {
      try {
        Files.createDirectories(packagesPath);
      } catch (IOException ex) {
        LOG.error("Failed to create directory {}", packagesPath, ex);
        return;
      }
    }

    // Ensure a valid packages.list exists
    writePackagesList();

    // Watching packages/ for changes
    startDirWatcher();
  }

  /**
   * Request a rewrite of packages.list
   *
   * The actual write will only happen UPDATE_DELAY ms after the last request
   * or when package manager operations have finished to avoid excessive writes.
   */
  private void requestUpdate() {

    updateAt = System.currentTimeMillis() + UPDATE_DELAY;

    if (updateRequestProcessor == null || !updateRequestProcessor.isAlive()) {
      updateRequestProcessor = new Thread(() -> {
        while (true) {
          while (updateAt > System.currentTimeMillis()
                 || packageManager.isRunning()) {
            try {
              Thread.sleep(100);
            } catch (InterruptedException ex) {
              LOG.info("Packages list update processor interrupted");
              return;
            }
          }
          writePackagesList();
          updateAt = Long.MAX_VALUE;  // wait for next request
        }
      });
      updateRequestProcessor.start();
    }
  }

  /**
   * Write the packages.list file
   *
   * The packages.list file consists of lines of paths (relative to nfs/root)
   * to SFS files under packages/ that are not marked as uninstalled.
   */
  private void writePackagesList() {
    List<String> uninstalledPaths = uninstalledContentTable.findAllSFContent()
                                    .stream()
                                    .map(PackageUninstalledContent::getPath)
                                    .collect(Collectors.toList());

    List<String> sfsPaths;
    try {
      sfsPaths = Files.walk(packagesPath, Integer.MAX_VALUE)
        .filter(Files::isRegularFile)
        .map(nfsRootPath::relativize)
        .sorted((left, right) -> {
          String left_name = left.getFileName().toString();
          String right_name = right.getFileName().toString();
          if (left_name.startsWith("base") && !right_name.startsWith("base"))
            return 1;
          if (!left_name.startsWith("base") && right_name.startsWith("base"))
            return -1;
          return left_name.compareTo(right_name);
        })
        .map(Path::toString)
        .filter(p -> p.endsWith(".sfs"))
        .filter(p -> !uninstalledPaths.contains(p))
        .collect(Collectors.toList());
    } catch (IOException ex) {
      LOG.error("Failed to read packages directory", ex);
      return;
    }

    try {
      Files.write(tempPackagesListPath, sfsPaths);
      Files.move(tempPackagesListPath, packagesListPath,
                 StandardCopyOption.ATOMIC_MOVE,
                 StandardCopyOption.REPLACE_EXISTING);
      LOG.info("packages.list updated");
    } catch (IOException ex) {
      LOG.error("Failed to write packages.list", ex);
    }
  }


  /** Register directory for watching for changes */
  private void watchDir(Path path) {
    try {
      path.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
      LOG.info("Watching {}", path);
    } catch (IOException ex) {
      LOG.error("Failed to watch directory {}", path, ex);
    }
  }


  /** Watch all dirs under packages/ and requestUpdate() upon any changes */
  private void startDirWatcher() {
    try {
      this.watchService = FileSystems.getDefault().newWatchService();
    } catch (IOException ex) {
      LOG.error("Failed to create WatchService", ex);
      return;
    }

    // Watch all existing directories
    try {
      Files.walk(packagesPath, Integer.MAX_VALUE)
            .filter(Files::isDirectory)
            .forEach(this::watchDir);
    } catch (IOException ex) {
      LOG.error("Failed to read packages directory", ex);
      return;
    }

    // Listen for changes, also watch any new dirs, and request updates
    new Thread(() -> {
      WatchKey watchKey;
      try {
        while ((watchKey = watchService.take()) != null) {
          while (watchKey != null) {
            Path dirPath = (Path)watchKey.watchable();
            for (WatchEvent<?> event: watchKey.pollEvents()) {
              Path path = dirPath.resolve((Path) event.context());
              if (Files.isDirectory(path)) watchDir(path);
            }
            watchKey.reset();
            watchKey = watchService.poll();
          }
          requestUpdate();
        }
      } catch (InterruptedException ex) {
        LOG.info("Packages directory watcher interrupted");
        return;
      } catch(Exception ex) {
        LOG.error("Error in packages directory watcher. Ignoring", ex);
      }
    }).start();
  }
}

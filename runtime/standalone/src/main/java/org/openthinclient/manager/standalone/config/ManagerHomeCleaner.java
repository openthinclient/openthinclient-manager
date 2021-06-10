package org.openthinclient.manager.standalone.config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ManagerHomeCleaner {

  private static final Logger LOG = LoggerFactory.getLogger(ManagerHomeCleaner.class);

  /** Max age in days for rolled over logs, reports and cached packages */
  private static long MAX_HISTORY_DAYS = 20;

  @Autowired
  private ManagerHome managerHome;

  @Autowired
  private PackageManager packageManager;

  private Path managerHomePath;

  public void clean() {
    LOG.info("Start cleaning manager home.");
    managerHomePath = managerHome.getLocation().toPath();
    long notBefore = OffsetDateTime.now()
                                    .minusDays(MAX_HISTORY_DAYS)
                                    .toEpochSecond() * 1000;
    cleanReports(notBefore);
    cleanLogs(notBefore);
    cleanPackagesCache(notBefore);
  }

  private static final Path REPORTS_PATH = Paths.get("nfs", "home", "reports");
  /**
   * Remove reports older than notBefore.
   *
   * Reports are created by tcos_exception_hook (in tcos-libs) if an unhandled
   * exception occurs on a thin client.
   *
   * @param notBefore - milliseconds since UNIX epoch, older reports will be removed
   */
  private void cleanReports(long notBefore) {
    File reportsDir = managerHomePath.resolve(REPORTS_PATH).toFile();
    if(reportsDir.isDirectory()) {
      for(File subdir : reportsDir.listFiles(File::isDirectory)) {
        for(File file : subdir.listFiles(file -> file.lastModified() < notBefore)) {
          deleteFile(file);
        }
      }
    }
  }

  /**
   * Remove all rolled over logs older than notBefore.
   *
   * This is necessary since logback's SizeAndTimeBasedRollingPolicy is
   * broken beyond usability. Only ZIPs are removed so no active log file is
   * ever touched.
   *
   * @param notBefore - milliseconds since UNIX epoch, older logs will be removed
   */
  private void cleanLogs(long notBefore) {
    Path logPath = managerHomePath.resolve("logs");
    deleteOldZips(logPath.toFile(), notBefore);
    deleteOldZips(logPath.resolve("syslog").toFile(), notBefore);
  }

  private void deleteOldZips(File dir, long notBefore) {
    if(!dir.isDirectory()) {
        LOG.warn("{} is not a directory", dir);
        return;
    }
    for(File file: dir.listFiles(file -> file.getName().endsWith(".zip")
                                          && file.lastModified() < notBefore)) {
      deleteFile(file);
    }
  }

  private static final Path ARCHIVES_PATH = Paths.get(
      "nfs", "root", "var", "cache", "archives");
  /**
   * Delete old files from package manager cache.
   *
   * If multiple files for a package exist, always keep the newest and remove
   * all others if they are older than notBefore.
   *
   * @param notBefore - milliseconds since UNIX epoch
   */
  private void cleanPackagesCache(long notBefore) {
    if(packageManager.isRunning()) {
      LOG.info("Package manager running. Skipping cleanup of package cache.");
      return;
    }

    File archivesDir = managerHomePath.resolve(ARCHIVES_PATH).toFile();

    // Collect all files for packages
    Map<String, List<File>> packages = new HashMap<>();
    for(File subdir : archivesDir.listFiles(File::isDirectory)) {
      for(File file : subdir.listFiles()) {
        String pkgName = file.getName().split("_", 2)[0];
        if(!packages.containsKey(pkgName)) {
          packages.put(pkgName, new ArrayList<>());
        }
        packages.get(pkgName).add(file);
      }
    }

    // Check whether multiple file exist and delete those that are too old.
    for(List<File> pkgFiles : packages.values()) {
      if(pkgFiles.size() > 1) {
        pkgFiles.sort(Comparator.comparing(File::lastModified).reversed());
        pkgFiles.remove(0);
        for(File file : pkgFiles) {
          if(file.lastModified() < notBefore) {
            deleteFile(file);
          }
        }
      }
    }
  }

  private void deleteFile(File file) {
    LOG.info("Removing {}", file);
    try {
      file.delete();
    } catch(SecurityException ex) {
      LOG.error(String.format("Failed to delete file %s", file.getName()), ex);
    }
  }
}

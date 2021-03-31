package org.openthinclient.manager.standalone.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.PostConstruct;

import com.google.common.base.Strings;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport.PackageReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport.PackageReportType;
import org.openthinclient.service.common.ServerIDFactory;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.ManagerHomeMetadata;
import org.openthinclient.web.pkgmngr.event.PackageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@DependsOn({"serviceManager", "liquibase"})
public class Migrations {
  private static Logger LOG = LoggerFactory.getLogger(Migrations.class);
  private static Version version2020 = Version.parse("2020");

  @Autowired
  private PackageManager pkgManager;
  @Autowired
  private LocationService locationService;

  private static String[] obsoletePackageNames = {
    "openthinclient-manager",
    "openthinclient-server-tftp",
    "openthinclient-server-ldap",
    "openthinclient-server-dhcp",
    "openthinclient-server-nfs",
    "tcos-scripts"
  };
  private static String[] obsoleteWithTcosLibs2020PackageNames = {
    "tcos-devices",
    "desktop"
  };
  private static String[] extensions = {
    "sfs",
    "md5",
    "changelog"
  };

  @Autowired
  public void setServerId(ManagerHome managerHome) {
    final ManagerHomeMetadata meta = managerHome.getMetadata();
    if (Strings.isNullOrEmpty(meta.getServerID())) {
      meta.setServerID(ServerIDFactory.create());
      meta.save();

    }
    LOG.info("Server ID: {}", meta.getServerID());
  }

  @PostConstruct
  public void init() {
    if(tcosLibs2020IsInstalled()) {
      updateLocationTimezone();
      removeObsoletePackageFiles(obsoleteWithTcosLibs2020PackageNames);
    }

    removeObsoletePackageFiles(obsoletePackageNames);
  }

  @EventListener
  public void onPackageEvent(PackageEvent ev) {
    if(tcosLibs2020Updated(ev.getReports())) {
      updateLocationTimezone();
      removeObsoletePackageFiles(obsoleteWithTcosLibs2020PackageNames);
    }
  }

  private void removeObsoletePackageFiles(String[] packageNames) {
    Path root = pkgManager.getConfiguration().getInstallDir().toPath();
    Path pkgPath = root.resolve("sfs/package");
    for(String pkgName: packageNames) {
      for(String ext: extensions) {
        try {
          Path path = pkgPath.resolve(pkgName + "." + ext);
          if(Files.exists(path)) {
            LOG.info("Deleting obsolete {}", path);
            Files.delete(path);
          }
        } catch(IOException ex) {}
      }
    }
  }

  private void updateLocationTimezone() {
    for(Location location : locationService.findAll()) {
      String tz = location.getValueLocal("Time.localtime");
      if(tz != null && tz.startsWith("posix/")) {
        LOG.info("Updating timezone for {}", location.getName());
        location.setValue("Time.localtime", tz.substring(6));
        locationService.save(location);
      }
    }
  }

  private boolean tcosLibs2020IsInstalled() {
    return pkgManager.getInstalledPackages()
            .stream()
            .anyMatch(this::isTcosLibs2020);
  }

  private boolean isTcosLibs2020(Package pkg) {
    return ("tcos-libs".equals(pkg.getName())
            && pkg.getVersion().compareTo(version2020) >= 0);
  }

  private boolean tcosLibs2020Updated(List<PackageReport> reports) {
    return reports.stream().anyMatch(report -> (
              isTcosLibs2020(report.getPackage())
              && ( report.getType().equals(PackageReportType.UPGRADE)
                  || report.getType().equals(PackageReportType.INSTALL) )
    ));
  }
}

package org.openthinclient.manager.standalone.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.PostConstruct;

import com.google.common.base.Strings;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.common.Events.LDAPImportEvent;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.service.HardwareTypeService;
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
  private static Version v2020 = Version.parse("2020");
  private static Version v2021 = Version.parse("2021");
  private static Version v2021b2 = Version.parse("2021.2~beta2~");
  private static Version v2025 = Version.parse("2025.1");

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private PackageManager pkgManager;
  @Autowired
  private HardwareTypeService hardwareTypeService;
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
  private static String[] obsoleteWithTcosLibs2025PackageNames = {
    "tcos-license"
  };
  private static String[] extensions = {
    "sfs",
    "md5",
    "changelog"
  };

  public void setServerId() {
    final ManagerHomeMetadata meta = managerHome.getMetadata();
    if (Strings.isNullOrEmpty(meta.getServerID())) {
      meta.setServerID(ServerIDFactory.create());
      meta.save();

    }
    LOG.info("Server ID: {}", meta.getServerID());
  }

  @PostConstruct
  public void init() {
    setServerId();

    runLDAPMigration();

    removeObsoletePackageFiles(obsoletePackageNames);
    removeObsoleteSyslogFiles();
  }

  @EventListener
  public void onPackageEvent(PackageEvent ev) {
    if(isUpdate(ev.getReports(), "tcos-libs", v2020)) {
      updateLocationTimezone();
      removeObsoletePackageFiles(obsoleteWithTcosLibs2020PackageNames);
    }
    if(isUpdate(ev.getReports(), "tcos-libs", v2021)) {
      fixLocationLanguageKey();
    }
    if(isUpdate(ev.getReports(), "tcos-libs", v2021b2)) {
      updateHardwaretypeBootOptions();
    }
    if(isUpdate(ev.getReports(), "tcos-libs", v2025)) {
      removeObsoletePackageFiles(obsoleteWithTcosLibs2025PackageNames);
    }
  }

  @EventListener
  public void runLDAPMigration(LDAPImportEvent ev) {
    runLDAPMigration();
  }

  public void runLDAPMigration() {
    if(isInstalled("tcos-libs", v2020)) {
      updateLocationTimezone();
      removeObsoletePackageFiles(obsoleteWithTcosLibs2020PackageNames);
    }

    if(isInstalled("tcos-libs", v2021)) {
      fixLocationLanguageKey();
    }

    if(isInstalled("tcos-libs", v2021b2)) {
      updateHardwaretypeBootOptions();
    }
  }

  private void removeObsoleteSyslogFiles() {
    File logDir = managerHome.getLocation().toPath().resolve("logs").toFile();
    if(logDir.isDirectory()) {
      for(File file: logDir.listFiles((d, name) -> name.startsWith("syslog."))) {
        try {
          file.delete();
        } catch(SecurityException ex) {
          LOG.error(String.format("Failed to delete obsolete log file %s",
                                  file.getName()),
                    ex);
        }
      }
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

  private void fixLocationLanguageKey() {
    for(Location location : locationService.findAll()) {
      String lang = location.getValueLocal("Lang.lang");
      if(lang != null && lang.equals("de_BE.UFT-8")) {
        LOG.info("Updating language de_BE.UFT-8 for {}", location.getName());
        location.setValue("Lang.lang", "de_BE.UTF-8");
        locationService.save(location);
      }
    }
  }

  private void updateHardwaretypeBootOptions() {
    for(HardwareType hwtype : hardwareTypeService.findAll()) {
      if(hwtype.containsValue("BootOptions.BootMode")) {
        continue;
      }
      if( !hwtype.containsValue("BootOptions.BootfileName") &&
          !hwtype.containsValue("BootOptions.BootLoaderTemplate")) {
        continue;
      }

      LOG.info("Updating boot mode for {}", hwtype.getName());

      String template = hwtype.getValueLocal("BootOptions.BootLoaderTemplate");
      // "translate" template to boot mode or keep unset/null for default
      if(template != null) {
        boolean isHTTPBoot = "template-http.txt".equals(template);
        hwtype.setValue("BootOptions.BootMode", isHTTPBoot? "fast" : "safe");
      }
      hwtype.removeValue("BootOptions.BootfileName");
      hwtype.removeValue("BootOptions.BootLoaderTemplate");
      hardwareTypeService.save(hwtype);
    }
  }

  private boolean is(Package pkg, String name, Version version) {
    return name.equals(pkg.getName())
            && version.compareTo(pkg.getVersion()) <= 0;
  }

  private boolean isInstalled(String name, Version version) {
    return pkgManager.getInstalledPackages()
            .stream()
            .anyMatch(pkg -> is(pkg, name, version));
  }

  private boolean isUpdate(List<PackageReport> reports, String name, Version version) {
    return reports.stream().anyMatch(report -> (
              is(report.getPackage(), name, version)
              && ( report.getType().equals(PackageReportType.UPGRADE)
                  || report.getType().equals(PackageReportType.INSTALL) )
    ));
  }
}

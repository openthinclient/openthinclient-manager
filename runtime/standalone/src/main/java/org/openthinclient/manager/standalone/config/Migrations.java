package org.openthinclient.manager.standalone.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.common.Events.LDAPImportEvent;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport.PackageReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport.PackageReportType;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.service.common.ServerIDFactory;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.ManagerHomeMetadata;
import org.openthinclient.splash.SplashServer;
import org.openthinclient.web.pkgmngr.event.PackageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Component
@DependsOn({"serviceManager", "liquibase"})
public class Migrations {
  private static Logger LOG = LoggerFactory.getLogger(Migrations.class);
  private static Version v2020 = Version.parse("2020");
  private static Version v2021 = Version.parse("2021");
  private static Version v2021b2 = Version.parse("2021.2~beta2~");
  private static Version v2025_1 = Version.parse("2025.1");
  private static Version v2511 = Version.parse("2511~");

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private PackageManager pkgManager;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private HardwareTypeService hardwareTypeService;
  @Autowired
  private DeviceService deviceService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private ApplicationContext applicationContext;
  @Autowired
  private SchemaProvider schemaProvider;

  @Value("${application.is-preview}")
  private boolean applicationIsPreview;
  @Value("${application.packages-update-version}")
  private String packagesUpdateVersion;

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

  private static Map<String, String> oldToNewAudioOptions = ImmutableMap
    .<String, String>builder()
      .put("pulseaudio.default-sink", "devices.default-sink")
      .put("pulseaudio.default-port", "devices.default-port")
      .put("pulseaudio.default-source", "devices.default-source")
      .put(
        "pulseaudio.sound-card-0-profile",
        "devices.sound-card-0-profile"
      )
      .put(
        "pulseaudio.sound-card-1-profile",
        "devices.sound-card-1-profile"
      )
      .put(
        "pulseaudio.sound-card-2-profile",
        "devices.sound-card-2-profile"
      )
      .put(
        "pulseaudio.sound-card-3-profile",
        "devices.sound-card-3-profile"
      )
      .put("pulseaudio.master-volume", "volume.master-output")
      .put(
        "pulseaudio.master-input-volume", "volume.master-input"
      )
      .put("pulseaudio.bell-volume", "bell.volume")
      .put("pulseaudio.custom-bell-sound", "bell.custom-sound")
      .build();

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

    autoUpdatePackages();

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
    if(isUpdate(ev.getReports(), "tcos-libs", v2025_1)) {
      removeObsoletePackageFiles(obsoleteWithTcosLibs2025PackageNames);
    }
    if(isUpdate(ev.getReports(), "tcos-libs", v2511)) {
      rewriteKioskModeSettings();
      separateAudioSettings();
      mergeSsoAndAutologinComponents();
    }
    if(isUpdate(ev.getReports(), "freerdp-git", v2511)) {
      separateFreeRdpAuthenticationOption();
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

    if(isInstalled("tcos-libs", v2511)) {
      rewriteKioskModeSettings();
      separateAudioSettings();
      mergeSsoAndAutologinComponents();
    }

    if(isInstalled("freerdp-git", v2511)) {
      separateFreeRdpAuthenticationOption();
    }
  }

  private void autoUpdatePackages() {
    ManagerHomeMetadata meta = managerHome.getMetadata();
    Version requiredVersion = Version.parse(packagesUpdateVersion);
    Version currentVersion = Version.parse(meta.getLastPackagesUpdateVersion());

    if (currentVersion.compareTo(requiredVersion) >= 0) {
      return;
    }

    LOG.info("Auto-updating OS packages");
    SplashServer.INSTANCE.setUpdatingPackages(true);
    try {
      if (runPackageUpdate()) {
        // Save successful update version, so we don't do it again
        meta.setLastPackagesUpdateVersion(packagesUpdateVersion);
        meta.save();
      }
    } catch(Exception ex) {
      LOG.error("Failed to auto-update packages", ex);
    } finally {
      SplashServer.INSTANCE.setUpdatingPackages(false);
    }
  }

  private boolean runPackageUpdate() throws InterruptedException,
                                            ExecutionException,
                                            CancellationException {
    // Get updatable packages
    pkgManager.updateCacheDB().get();  // wait for package list update
    Collection<Package> updatablePkgs;
    updatablePkgs = pkgManager.getUpdateablePackages(applicationIsPreview);
    if(updatablePkgs.isEmpty()) {
      LOG.warn("No updatable packages found");
      return false;
    }

    Map<String, Package> latestPackages = new HashMap<>();
    for (Package pkg : updatablePkgs) {
      String pkgName = pkg.getName();
      Version pkgVersion = pkg.getVersion();
      Package latest = latestPackages.get(pkgName);
      if (latest == null || pkgVersion.compareTo(latest.getVersion()) > 0) {
        latestPackages.put(pkgName, pkg);
      }
    }

    // Create installation operation
    PackageManagerOperation op = pkgManager.createOperation();
    latestPackages.values().forEach(op::install);
    op.resolve();

    // Run installation and report progress to splash server.
    // (Note: We don't use a ProgressReceiver on the ListenableProgressFuture
    // which is extremely verbose and would overwhelm the browser displaying
    // the progress with too many updates.)
    ListenableProgressFuture<PackageManagerOperationReport> installation;
    installation = pkgManager.execute(op);

    PackageManagerOperationReport installationReport = null;
    while(installationReport == null) {
      SplashServer.INSTANCE.setProgress(installation.getProgress());
      try {
        installationReport = installation.get(500, MILLISECONDS);
      } catch(java.util.concurrent.TimeoutException ex) {}
    }

    // Notify listeners about the package update and reload schemas
    applicationContext.publishEvent(new PackageEvent(installationReport));
    clientService.reloadAllSchemas();

    return true;
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

  private void rewriteKioskModeSettings() {
    for(Application application : applicationService.findAll()) {
      Schema schema;
      try {
        schema = application.getSchema(application.getRealm());
      } catch (Exception ex) {
        LOG.error("Failed to get schema for app {}: {}",
                  application.getName(), ex.getMessage());
        continue;
      }

      if (!schema.getName().equals("desktop")) {
        continue;
      }

      if (!application.containsValue("session")) {
        continue;
      }

      LOG.info("Updating kiosk mode settings for {}", application.getName());

      String session = application.getValueLocal("session");
      boolean anyKioskMode = !session.equals("mate");

      application.setValue("kiosk_mode", String.valueOf(anyKioskMode));

      if (session.equals("kiosk")) {
        application.setValue("panel.panel", "false");
      } else if (session.equals("kiosk-panel")) {
        application.setValue("panel.panel", "true");
        application.setValue("panel.menu", "false");
        application.setValue("panel.ip", "false");
        application.setValue("panel.cpu_monitor", "false");
        application.setValue("panel.systray", "false");
        application.setValue("panel.clock", "false");
      }

      application.removeValue("session");
      applicationService.save(application);
    }
  }

  private void separateFreeRdpAuthenticationOption() {
    for(Application application : applicationService.findAll()) {
      Schema schema;
      try {
        schema = application.getSchema(application.getRealm());
      } catch (Exception ex) {
        LOG.error("Failed to get schema for app {}: {}",
                  application.getName(), ex.getMessage());
        continue;
      }

      if (!schema.getName().equals("freerdp-git")) {
        continue;
      }

      String authMethod = application.getValueLocal(
        "Application.Account.Authentication"
      );

      if (authMethod != null && !authMethod.equals("default")) {
        continue;
      }

      String user = application.getValueLocal("Application.Account.User");
      String password = application.getValueLocal(
        "Application.Account.Password"
      );

      String newAuthMethod;
      if (user != null && password != null
          && !user.trim().isEmpty() && !password.trim().isEmpty()) {
        newAuthMethod = "pre-configured";
      } else {
        newAuthMethod = "auth-manual-simple";
      }

      application.setValue(
        "Application.Account.Authentication", newAuthMethod
      );
      applicationService.save(application);
    }
  }

  private void separateAudioSettings() {
    for(Application application : applicationService.findAll()) {
      Schema schema;
      try {
        schema = application.getSchema(application.getRealm());
      } catch (Exception ex) {
        LOG.error("Failed to get schema for app {}: {}",
                  application.getName(), ex.getMessage());
        continue;
      }

      if (!schema.getName().equals("desktop")) {
        continue;
      }

      LOG.info(
        "Separating audio settings for desktop application '{}'",
        application.getName()
      );

      Schema audioSchema = schemaProvider.getSchema(Application.class, "audio");
      Application audio = new Application();
      audio.setSchema(audioSchema);
      audio.setName(String.format("Audio - %s", application.getName()));
      audio.setMembers(new HashSet<DirectoryObject>(application.getMembers()));

      boolean hasValuesSet = false;

      for(Entry<String, String> entry : oldToNewAudioOptions.entrySet()) {
        String oldValName = entry.getKey();
        String newValName = entry.getValue();

        String value = application.getValueLocal(oldValName);

        if (value == null || value.isEmpty()) {
          continue;
        }

        hasValuesSet = true;
        audio.setValue(newValName, value);
        application.removeValue(oldValName);
      }

      if (hasValuesSet) {
        applicationService.save(audio);
        applicationService.save(application);
      }
    }
  }

  private void mergeSsoAndAutologinComponents() {
    try {
      if (!mergeSsoAndAutologinComponentsConvert()) {
        LOG.info("No components converted");
        return;
      }
      mergeSsoAndAutologinComponentsUnlink();
      mergeSsoAndAutologinComponentsDelete();
    } catch (DirectoryException ex) {
      LOG.error("Merging sso and autologin components failed", ex);
    }
  }

  // First step of the `mergeSsoAndAutologinComponents` migration.
  // Converts all sso and autologin components to login components.
  private boolean mergeSsoAndAutologinComponentsConvert() throws DirectoryException {
    boolean anyConverted = false;
    Schema loginSchema = schemaProvider.getSchema(Device.class, "login");

    for (Device device : deviceService.findAll()) {
      String schemaName = device.getSchemaName2();

      if (!(schemaName.equals("sso") || schemaName.equals("autologin"))) {
        continue;
      }

      anyConverted = true;
      LOG.info(
        "Converting {} device '{}' to login device.",
        schemaName, device.getName()
      );

      Device login = new Device();
      login.setSchema(loginSchema);
      login.setName(device.getName());
      login.setMembers(new HashSet<DirectoryObject>(device.getMembers()));

      if (schemaName.equals("sso")) {
        login.setValue("login.type", "sso");
      } else {
        login.setValue("login.type", "autologin");
      }

      deviceService.delete(device);
      deviceService.save(login);
    }

    return anyConverted;
  }

  // Second step of the `mergeSsoAndAutologinComponents` migration.
  // Unlinks some login components from clients and/or hardware types.
  private void mergeSsoAndAutologinComponentsUnlink() {
    Schema loginSchema = schemaProvider.getSchema(Device.class, "login");

    for (HardwareType hwType : hardwareTypeService.findAll()) {
      Set<Device> devices = hwType.getDevices();
      Set<Device> autologinDevices = new HashSet<>();
      Set<Device> ssoDevices = new HashSet<>();

      for (Device device : devices) {
        String schema = device.getSchemaName2();

        if (!schema.equals("login")) {
          continue;
        }

        String loginType = device.getValueLocal("login.type");

        if (loginType == null | loginType.equals("autologin")) {
          autologinDevices.add(device);
        } else if (loginType.equals("sso")) {
          ssoDevices.add(device);
        }
      }

      boolean hwTypeHasAutologin = false;

      if (!autologinDevices.isEmpty()) {
        devices.removeAll(autologinDevices);
        devices.removeAll(ssoDevices);
        hwTypeHasAutologin = true;
      } else if (!ssoDevices.isEmpty()) {
        ssoDevices.remove(ssoDevices.iterator().next());
        devices.removeAll(ssoDevices);
      } else {
        Device credentialsDevice = new Device();
        credentialsDevice.setSchema(loginSchema);
        credentialsDevice.setName(String.format("Login - %s", hwType.getName()));
        credentialsDevice.setValue("login.type", "credentials");
        deviceService.save(credentialsDevice);
        devices.add(credentialsDevice);
      }

      hardwareTypeService.save(hwType);

      for (Client client : hwType.getMembers()) {
        Set<Device> clientDevices = client.getDevices();
        Set<Device> clientAutologinDevices = new HashSet<>();
        Set<Device> clientSsoDevices = new HashSet<>();

        for (Device device : clientDevices) {
          String schema = device.getSchemaName2();

          if (!schema.equals("login")) {
            continue;
          }

          String loginType = device.getValueLocal("login.type");

          if (loginType == null | loginType.equals("autologin")) {
            clientAutologinDevices.add(device);
          } else if (loginType.equals("sso")) {
            clientSsoDevices.add(device);
          }
        }

        if (!clientAutologinDevices.isEmpty()) {
          clientDevices.removeAll(clientSsoDevices);

          if (hwTypeHasAutologin) {
            clientDevices.removeAll(clientAutologinDevices);
          } else {
            clientAutologinDevices.remove(clientAutologinDevices.iterator().next());
            clientDevices.removeAll(clientAutologinDevices);
          }
        } else if (!clientSsoDevices.isEmpty()) {
          clientSsoDevices.remove(clientSsoDevices.iterator().next());
          clientDevices.removeAll(clientSsoDevices);
        }

        clientService.save(client);
      }
    }
  }

  // Third step of the `mergeSsoAndAutologinComponents` migration.
  // Delete login components that are not linked to any client or hardware type.
  private void mergeSsoAndAutologinComponentsDelete() throws DirectoryException {
    for (Device device : deviceService.findAll()) {
      if (!device.getSchemaName2().equals("login")) {
        continue;
      }

      if (device.getMembers().isEmpty()) {
        deviceService.delete(device);
        LOG.info("Deleting login device {}.", device.getName());
      }
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

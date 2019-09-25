package org.openthinclient.sysreport.generate;

import org.apache.commons.lang3.SystemUtils;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.sysreport.AbstractReport;
import org.openthinclient.service.common.home.impl.ApplianceConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ServerReportContributor implements ReportContributor<AbstractReport> {

  private static final String VM_VERSION_PATH = "/usr/local/share/openthinclient/openthinclient-vm-version";

  /**
   * A whitelist containing all environment variables that will be included including their value.
   */
  public static final Set<String> ENV_WHITELIST;

  static {
    Set<String> whitelist = new HashSet<>();

    whitelist.add("LC_CTYPE".toLowerCase());
    whitelist.add("LANG".toLowerCase());
    whitelist.add("TERM".toLowerCase());

    ENV_WHITELIST = Collections.unmodifiableSet(whitelist);
  }

  private final ManagerHome managerHome;

  public ServerReportContributor(ManagerHome managerHome) {
    this.managerHome = managerHome;
  }

  @Override
  public void contribute(AbstractReport report) {

    report.getServer().setServerId(managerHome.getMetadata().getServerID());

    boolean isAppliance = false;
    String applianceVersion = "";
    try {
      isAppliance = ApplianceConfiguration.get(managerHome).isEnabled();
      applianceVersion = new String(Files.readAllBytes(Paths.get(VM_VERSION_PATH)));
    } catch (IOException ex) {
    }
    report.getServer().setIsAppliance(isAppliance);
    report.getServer().setApplianceVersion(applianceVersion);

    report.getServer().getOS().setArch(SystemUtils.OS_ARCH);
    report.getServer().getOS().setName(SystemUtils.OS_NAME);
    report.getServer().getOS().setVersion(SystemUtils.OS_VERSION);

    final Map<String, String> env = System.getenv();

    final List<String> keys = new ArrayList<>();
    final HashMap<String, String> filtered = new HashMap<>();
    env.forEach((key, value) -> {
      if (ENV_WHITELIST.contains(key.toLowerCase())) {
        filtered.put(key, value);
      }
      // adding all environment variable keys
      keys.add(key);
    });
    report.getServer().setEnvironment(filtered);
    report.getServer().setEnvironmentKeys(keys);


  }
}

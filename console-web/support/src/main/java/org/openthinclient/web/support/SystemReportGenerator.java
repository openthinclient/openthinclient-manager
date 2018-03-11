package org.openthinclient.web.support;

import com.google.common.primitives.Bytes;

import org.apache.commons.lang3.SystemUtils;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.sysreport.SystemReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SystemReportGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(SystemReportGenerator.class);

  private final ManagerHome managerHome;
  private final List<ReportContributor> contributors;

  public SystemReportGenerator(ManagerHome managerHome) {
    this.managerHome = managerHome;
    contributors = new ArrayList<>();
    contributors.add(new NetworkReportContributor());
  }

  public List<ReportContributor> getContributors() {
    return contributors;
  }

  public SystemReport generateReport() {

    final SystemReport report = new SystemReport();

    report.getSubmitter().setSubmitterType(SystemReport.SubmitterType.AUTOMATED);
    report.getSubmitter().setName("openthinclient.org Manager, Report Generator");

    report.getServer().setServerId(managerHome.getMetadata().getServerID());

    report.getServer().getOS().setArch(SystemUtils.OS_ARCH);
    report.getServer().getOS().setName(SystemUtils.OS_NAME);
    report.getServer().getOS().setVersion(SystemUtils.OS_VERSION);

    report.getServer().setEnvironment(System.getenv());

    final Map<String, String> properties = new HashMap<>();
    report.getManager().getJava().setProperties(properties);

    System.getProperties().forEach((key, value) -> {
      properties.put(""+key, ""+value);
    });

    for (ReportContributor contributor : contributors) {
      contributor.contribute(report);
    }

    return report;
  }

  public static String toMacAddressString(byte[] address) {
    return Bytes.asList(address).stream()
            .map(b -> String.format("%02x", b))
            .collect(Collectors.joining(":"));
  }

  public interface ReportContributor {
    void contribute(SystemReport report);
  }

  public static class NetworkReportContributor implements ReportContributor {

    @Override
    public void contribute(SystemReport report) {
      try {
        for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {

          final SystemReport.NetworkInterface target = new SystemReport.NetworkInterface();

          target.setDisplayName(networkInterface.getDisplayName());
          target.setName(networkInterface.getName());
          if (networkInterface.getHardwareAddress() != null)
            target.setHardwareAddress(toMacAddressString(networkInterface.getHardwareAddress()));
          for (InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
            target.getAddresses().add(inetAddress.getHostAddress());
          }

          report.getNetwork().getInterfaces().add(target);
        }
      } catch (SocketException e) {
        LOGGER.error("Failed to dump interfaces", e);
      }

    }
  }
}

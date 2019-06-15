package org.openthinclient.sysreport.generate;

import org.openthinclient.sysreport.SystemReport;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public class NetworkInterfaceDetailsContributor extends AbstractNetworkInterfaceContributor<SystemReport> {

  @Override
  protected void contribute(SystemReport report, NetworkInterface networkInterface) throws SocketException {
    final SystemReport.NetworkInterfaceDetails target = new SystemReport.NetworkInterfaceDetails();

    target.setDisplayName(networkInterface.getDisplayName());
    target.setName(networkInterface.getName());

    if (networkInterface.getHardwareAddress() != null)
      target.setHardwareAddress(SystemReportGenerator.toMacAddressString(networkInterface.getHardwareAddress()));
    for (InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
      target.getAddresses().add(inetAddress.getHostAddress());
    }

    report.getNetwork().getInterfaces().add(target);
  }
}

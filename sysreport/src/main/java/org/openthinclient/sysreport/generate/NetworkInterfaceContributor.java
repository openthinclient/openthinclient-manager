package org.openthinclient.sysreport.generate;

import org.openthinclient.sysreport.AbstractReport;
import org.openthinclient.sysreport.StatisticsReport;

import java.net.NetworkInterface;

public class NetworkInterfaceContributor extends AbstractNetworkInterfaceContributor<StatisticsReport> {
  @Override
  protected void contribute(StatisticsReport report, NetworkInterface networkInterface) {

    final AbstractReport.NetworkInterface model = new AbstractReport.NetworkInterface();
    model.setDisplayName(networkInterface.getDisplayName());
    model.setName(networkInterface.getName());
    report.getNetwork().getInterfaces().add(model);

  }
}

package org.openthinclient.sysreport.generate;

import org.openthinclient.sysreport.AbstractReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public abstract class AbstractNetworkInterfaceContributor<T extends AbstractReport> implements ReportContributor<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNetworkInterfaceContributor.class);

  @Override
  public void contribute(T report) {
    try {
      for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
        contribute(report, networkInterface);
      }
    } catch (SocketException e) {
      LOGGER.error("Failed to dump interfaces", e);
    }

  }

  protected abstract void contribute(T report, NetworkInterface networkInterface) throws SocketException;
}

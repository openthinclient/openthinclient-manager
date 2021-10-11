package org.openthinclient.wizard.inventory;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NetworkInterfaces {

  /**
   * A purely virtual mac address.
   */
  public static final String VIRTUAL_MAC_ADDRESS = "00-07-E9-37-2D-02";

  /**
   * The default prefix used by VMware products for MAC addresses of virtual network adapters.
   */
  public static final String VMWARE_MAC_PREFIX = "00:50:56";

  /**
   * The default prefix used by parallels products for MAC addresses of virtual network adapters.
   */
  public static final String PARALLELS_MAC_PREFIX = "00:1C:42";

  private final List<NetworkInterface> loopbackInterfaces;
  private final List<NetworkInterface> nonLoopbackInterfaces;

  public NetworkInterfaces() {
    loopbackInterfaces = new ArrayList<>();
    nonLoopbackInterfaces = new ArrayList<>();
  }

  /**
   * A list containing all loopback network interfaces.
   */
  public List<NetworkInterface> getLoopbackInterfaces() {
    return loopbackInterfaces;
  }

  /**
   * All non loopback interfaces.
   */
  public List<NetworkInterface> getNonLoopbackInterfaces() {
    return nonLoopbackInterfaces;
  }

  public Collection<NetworkInterface> getAllInterfaces() {

    final ArrayList<NetworkInterface> all = new ArrayList<>();
    all.addAll(loopbackInterfaces);
    all.addAll(nonLoopbackInterfaces);
    return all;

  }

  /**
   * Creates a default string representation of a hardware address (MAC).
   *
   * @param nic the {@link NetworkInterface} for which the MAC shall be determined
   * @return the MAC in the typical "00:11:22:33:44:55" format, or <code>null</code> if no hardware address could be found.
   */
  public String getHardwareAddressString(NetworkInterface nic) {

    final byte[] hardwareAddress;
    try {
      hardwareAddress = nic.getHardwareAddress();
    } catch (SocketException e) {
      throw new RuntimeException("Failed to access the hardware address of the given network interface: " + nic.getDisplayName(), e);
    }

    if (hardwareAddress == null) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();

    for (byte b : hardwareAddress) {
      if (sb.length() > 0)
        sb.append(':');
      sb.append(String.format("%02X", b));
    }

    return sb.toString();
  }

}

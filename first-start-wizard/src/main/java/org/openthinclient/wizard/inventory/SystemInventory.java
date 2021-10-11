package org.openthinclient.wizard.inventory;

/**
 * A collection of information about a system.
 */
public class SystemInventory {

  private final NetworkInterfaces networkInterfaces;

  /* default */ SystemInventory(NetworkInterfaces networkInterfaces) {
    this.networkInterfaces = networkInterfaces;
  }

  public NetworkInterfaces getNetworkInterfaces() {
    return networkInterfaces;
  }
}

package org.openthinclient.advisor.inventory;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;

public class SystemInventoryFactory {

  private final AsyncListenableTaskExecutor executorService;

  public SystemInventoryFactory(AsyncListenableTaskExecutor executorService) {
    this.executorService = executorService;
  }

  public ListenableFuture<SystemInventory> determineSystemInventory() {

    return executorService.submitListenable(() -> new SystemInventory(collectNetworkInterfaces()));

  }

  private NetworkInterfaces collectNetworkInterfaces() throws SocketException {
    final NetworkInterfaces nics = new NetworkInterfaces();

    final ArrayList<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

    // find all loopback devices
    interfaces.stream().filter(this::isLoopbackDevice).forEach(nics.getLoopbackInterfaces()::add);
    // find all non loopback devices
    interfaces.stream().filter(this::isNonLoopbackDevice).forEach(nics.getNonLoopbackInterfaces()::add);

    return nics;
  }

  private boolean isNonLoopbackDevice(NetworkInterface networkInterface) {
    return !isLoopbackDevice(networkInterface);
  }

  private boolean isLoopbackDevice(NetworkInterface networkInterface) {

    return Collections.list(networkInterface.getInetAddresses())
            .stream()
            .anyMatch(inetAddress -> inetAddress.isLoopbackAddress() || inetAddress.getHostAddress().equals("127.0.0.1"));

  }


}

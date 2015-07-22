package org.openthinclient.advisor.check;

import org.openthinclient.advisor.inventory.SystemInventory;
import org.openthinclient.advisor.inventory.SystemInventoryFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class CheckNetworkInferfaces extends AbstractCheck<CheckNetworkInferfaces.NetworkInterfacesCheckSummary> {

  private final SystemInventory systemInventory;

  public CheckNetworkInferfaces(SystemInventory systemInventory) {
    super("Network interfaces supported", "");
    this.systemInventory = systemInventory;
  }

  // This is a simple test method. Will not be used in the delivered product, but could be of use for testing.
  public static void main(String[] args) throws ExecutionException, InterruptedException {

    final SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
    final CheckExecutionEngine engine = new CheckExecutionEngine(taskExecutor);

    final SystemInventoryFactory inventoryFactory = new SystemInventoryFactory(taskExecutor);

    final ListenableFuture<SystemInventory> systemInventory = inventoryFactory.determineSystemInventory();

    engine.execute(new CheckNetworkInferfaces(systemInventory.get())).onResult(r -> {

      System.out.println(r.getType());
      System.out.println(r.getValue().getDeviceSummary());
      System.out.println();
      System.out.println(r.getValue().getMessage());

    });

  }

  @Override
  protected CheckExecutionResult<NetworkInterfacesCheckSummary> perform() {

    final StringBuilder sbDevices = new StringBuilder();

    final Consumer<NetworkInterface> dumpNetworkInterfacesConsumer = n -> {
      sbDevices.append(String.format("%s (%s)", n.getName(), n.getDisplayName()));
      sbDevices.append(" [");
      sbDevices.append(systemInventory.getNetworkInterfaces().getHardwareAddressString(n));
      sbDevices.append("]");
      sbDevices.append("\r\n");
      Collections.list(n.getInetAddresses()).forEach(address -> {
        sbDevices.append(String.format("- %s", address.getHostAddress()));
        sbDevices.append("\r\n");
      });
    };

    sbDevices.append("System Information Summary\r\n");
    sbDevices.append("Loopback interfaces ------------------------------\r\n");
    systemInventory.getNetworkInterfaces().getLoopbackInterfaces().forEach(
            dumpNetworkInterfacesConsumer
    );
    sbDevices.append("Network interfaces -------------------------------\r\n");
    systemInventory.getNetworkInterfaces().getNonLoopbackInterfaces().forEach(
            dumpNetworkInterfacesConsumer
    );

    final CheckExecutionResult.CheckResultType type;
    final String message;

    if (systemInventory.getNetworkInterfaces().getAllInterfaces().size() == 0) {
      type = CheckExecutionResult.CheckResultType.FAILED;
      message = "No network devices detected on this system.";
    }
    // we require at least one non loopback interface to be available on the system.
    else if (systemInventory.getNetworkInterfaces().getNonLoopbackInterfaces().size() == 0) {
      type = CheckExecutionResult.CheckResultType.FAILED;
      message = "No non loopback network devices has been detected on your system.";
    }

    // the OTC manager works best if there is only one single non loopback device
    else if (systemInventory.getNetworkInterfaces().getNonLoopbackInterfaces().size() == 1) {
      type = CheckExecutionResult.CheckResultType.SUCCESS;
      message = "";
    }
    // we have more than only one network device. Things could become complicated.
    else {
      type = CheckExecutionResult.CheckResultType.WARNING;
      message = " non loopback network interfaces detected on your system. Only a single network device is recommended for use with the openthinclient manager.";
    }

    return new CheckExecutionResult<>(type, new NetworkInterfacesCheckSummary(message, sbDevices.toString()));
  }

  public static final class NetworkInterfacesCheckSummary {
    private final String message;
    private final String deviceSummary;

    public NetworkInterfacesCheckSummary(String message, String deviceSummary) {
      this.message = message;
      this.deviceSummary = deviceSummary;
    }

    public String getMessage() {
      return message;
    }

    public String getDeviceSummary() {
      return deviceSummary;
    }
  }


}

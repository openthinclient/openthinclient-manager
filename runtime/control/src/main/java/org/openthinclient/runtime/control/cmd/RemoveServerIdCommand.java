package org.openthinclient.runtime.control.cmd;

import org.kohsuke.args4j.Option;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;

import java.nio.file.Path;

public class RemoveServerIdCommand extends AbstractCommand<RemoveServerIdCommand.Options> {

  public RemoveServerIdCommand() {
    super("rm-server-id");
  }

  @Override
  public Options createOptionsObject() {
    return new Options();
  }

  @Override
  public void execute(Options options) throws Exception {

    // FIXME validate that the manager server is not running at the moment

    final ManagerHomeFactory managerHomeFactory = new ManagerHomeFactory();
    managerHomeFactory.setManagerHomeDirectory(options.homePath.toFile());

    if (!managerHomeFactory.isManagerHomeValidAndInstalled()) {
      System.err.println("Not a valid manager home directory: " + options.homePath.toAbsolutePath());
      System.exit(1);
      return;
    }

    final ManagerHome managerHome = managerHomeFactory.create();

    managerHome.getMetadata().setServerID(null);
    managerHome.getMetadata().save();

  }

  public class Options {
    @Option(name = "--home", required = true, metaVar = "DIR", usage = "The target manager home directory")
    public Path homePath;
  }
}
